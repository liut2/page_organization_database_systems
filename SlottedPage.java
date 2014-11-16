import java.nio.*;
import java.util.Arrays;

/** 
 * written by Tao Liu
 */

/**
 * Slotted file page. This is a wrapper around a traditional Page that
 * adds the appropriate struture to it.
 *
 * @author Dave Musicant, with considerable inspiration from the UW-Madison
 * Minibase project
 */
public class SlottedPage
{
    public static class PageFullException extends RuntimeException {};
    public static class BadSlotIdException extends RuntimeException {};
    public static class BadPageIdException extends RuntimeException {};

    private static class SlotArrayOutOfBoundsException
        extends RuntimeException {};

    /**
     * Value to use for an invalid page id.
     */
    public static final int INVALID_PAGE = -1;
    public static final int SIZE_OF_INT = 4;

    private byte[] data;
    private IntBuffer intBuffer;
    private int intBufferLength;
    private int headerSize;

    /**
     * Constructs a slotted page by wrapping around a page object already
     * provided.
     * @param page the page to be wrapped.
     */
    public SlottedPage(Page page)
    {
        data = page.data;
        intBuffer = (ByteBuffer.wrap(data)).asIntBuffer();
        intBufferLength = data.length / SIZE_OF_INT;
        headerSize = 20;
        intBuffer.put(4,data.length - 1 );
    }

    /**
     * Initializes values on the heap file page as necessary. This is
     * separated out from the constructor since it actually modifies
     * the page at hand, where as the constructor simply sets up the
     * mechanism.
     */
    public void init()
    {
        intBuffer.put(0,0);
    }


    /**
     * Sets the page id.
     * @param pageId the new page id.
     */
    public void setPageId(int pageId)
    {
        if (pageId == INVALID_PAGE) { throw new BadPageIdException(); }
        else { intBuffer.put(1,pageId); }
    }

    /**
     * Gets the page id.
     * @return the page id.
     */
    public int getPageId()
    {
        return intBuffer.get(1);
    }

    /**
     * Sets the next page id.
     * @param pageId the next page id.
     */
    public void setNextPageId(int pageId)
    {
        if (pageId == INVALID_PAGE) { throw new BadPageIdException(); }
        else { intBuffer.put(2,pageId); } 
    }

    /**
     * Gets the next page id.
     * @return the next page id.
     */
    public int getNextPageId()
    {
        return intBuffer.get(2);
    }

    /**
     * Sets the previous page id.
     * @param pageId the previous page id.
     */
    public void setPrevPageId(int pageId)
    {
        if (pageId == INVALID_PAGE) { throw new BadPageIdException(); }
        else { intBuffer.put(3,pageId); } 
    }

    /**
     * Gets the previous page id.
     * @return the previous page id.
     */
    public int getPrevPageId()
    {
        return intBuffer.get(3);
    }

    /**
     * Determines how much space, in bytes, is actually available on the page,
     * which depends on whether or not a new slot in the slot array is
     * needed. If a new spot in the slot array is needed, then the amount of
     * available space has to take this into consideration. In other words, the
     * space you need for the addition to the slot array shouldn't be included
     * as part of the available space, because from the user's perspective, it
     * isn't available for adding data.
     * @return the amount of available space in bytes
     */
    public int getAvailableSpace()
    {
       int startOfFree = 19 + (intBuffer.get(0) * 8);
        return intBuffer.get(4) - startOfFree;
    }
        

    /**
     * Dumps out to the screen the # of entries on the page, the location where
     * the free space starts, the slot array in a readable fashion, and the
     * actual contents of each record. (This method merely exists for debugging
     * and testing purposes.)
    */ 
    public void dumpPage()
    {
        
        System.out.println("The Number of Entries On Page Is: "+ Integer.toString(intBuffer.get(0)));
        System.out.println("The Location the Free Space Starts Is: "+Integer.toString(intBuffer.get(4)));

        int end_of_rids = intBuffer.get(0) + 4;
        int start_of_value;
        int end_of_value;
        int count_of_zeros = 0;
        
        for ( int i = 5; i <= end_of_rids + count_of_zeros; i++) {
            if (intBuffer.get(i) != 0) {
                RID temp = new RID(getPageId(),intBuffer.get(i));
                byte[] tempArray = getRecord(temp);
                System.out.println("The Value at SlotNum "+Integer.toString(intBuffer.get(i))+" is "+ Arrays.toString(getRecord(temp)));
        
            } else if ( intBuffer.get(i) == 0 ) {
                count_of_zeros++;
            }
        }
        System.out.println(Arrays.toString(data));
    }

    /**
     * Inserts a new record onto the page.
     * @param record the record to be inserted. A copy of the data is
     * placed on the page.
     * @return the RID of the new record 
     * @throws PageFullException if there is not enough room for the
     * record on the page.
    */
    public RID insertRecord(byte[] record)
    {
        if (record.length > getAvailableSpace()) {
            throw new PageFullException();
        }
        int start_of_insertion = intBuffer.get(4) - record.length + 1;
        int record_index = 0;
        intBuffer.put(intBuffer.get(0) + 5, start_of_insertion);
        intBuffer.put(0, intBuffer.get(0)+1);
        intBuffer.put(4, start_of_insertion-1);
        for(int i = start_of_insertion; i < start_of_insertion + record.length ; i++ ){
            data[i] = record[record_index];
            record_index++;
        }
        RID rid = new RID(intBuffer.get(1), start_of_insertion);
        return rid;
    }
    
    
    private int prevSlotArrayIndex(int index) {
        if (index-1 == 4) {
            return 0;
        }
        else {
            int output = 0;
            for (int i = index-1; i > 4; i--) {
                if (intBuffer.get(i) != 0) {
                    output = i;
                }
            }
        return output;
        }
    }
    /**
     * Deletes the record with the given RID from the page, compacting
     * the hole created. Compacting the hole, in turn, requires that
     * all the offsets (in the slot array) of all records after the
     * hole be adjusted by the size of the hole, because you are
     * moving these records to "fill" the hole. You should leave a
     * "hole" in the slot array for the slot which pointed to the
     * deleted record, if necessary, to make sure that the rids of the
     * remaining records do not change. The slot array should be
     * compacted only if the record corresponding to the last slot is
     * being deleted.
     * @param rid the RID to be deleted.
     * @return true if successful, false if the rid is actually not
     * found on the page.
    */
    public boolean deleteRecord(RID rid)
    {
        if (rid.pageId != getPageId()) {
            return false;
        }
        
        int pageId = rid.pageId;
        int slotNum = rid.slotNum;
        int lengthOfDelete = 0;
        int prevSlotNum = 0;
        int count_of_zeros = 0;
        int curRIDPos = 0;
        boolean isFound = false;
        
        // Iterate over all RIDS until we find the one we wish to delete
        for (int i = 5; i < intBuffer.get(0) + count_of_zeros + 5; i++){
            if (intBuffer.get(i) == 0){
                count_of_zeros++;
            } else if (rid.slotNum == intBuffer.get(i)) {

                // If deleting most recent entry i.e. slotNum = end of free space
                if (intBuffer.get(i) == intBuffer.get(4) + 1) {
                    for ( int j = i - 1; j > 4 ; j-- ) {
                        if (intBuffer.get(j) != 0) {
                            // Calculate Length of Record being Deleted
                            prevSlotNum = intBuffer.get(j);
                            lengthOfDelete = prevSlotNum - rid.slotNum;
                            break;
                        }
                    }
                    // Clear out record
                    for ( int k = rid.slotNum; k < rid.slotNum + lengthOfDelete; k++){
                        data[k] = 0;
                    }
                    // Update Header
                    intBuffer.put(4,intBuffer.get(4) + lengthOfDelete);
                    intBuffer.put(i,0);
                    intBuffer.put(0,intBuffer.get(0)-1);
                    break;
                // If deleting other records
                } else {
                    curRIDPos = i;
                    int prevSlotArrayInd = 0;
                    // finding previous slotNum to get length
                    prevSlotArrayInd = prevSlotArrayIndex(i);
                    if (prevSlotArrayInd == 0) {
                        prevSlotNum = data.length;
                    } else {
                        prevSlotNum = intBuffer.get(prevSlotArrayInd);
                    }
                    // Calculate Length of Record being Deleted
                    lengthOfDelete = prevSlotNum - rid.slotNum;
                    // Update Header
                    intBuffer.put(i,0);
                    intBuffer.put(0,intBuffer.get(0)-1);
                    isFound = true;
                    break;
                }
            }
        }

        int nextSlotArrayInd = nextSlotArrayIndex(curRIDPos);
        
        // iterate fowards from slotNum to end of free space
        if (isFound) {
            for (int i = rid.slotNum-1; i >= intBuffer.get(4); i--) {
                // Shift Values
                data[i+lengthOfDelete] = data[i];
                data[i] = 0;
                // If we reach the next RID
                if (i == intBuffer.get(nextSlotArrayInd)) {
                    intBuffer.put(nextSlotArrayInd, i+lengthOfDelete);
                    nextSlotArrayInd = nextSlotArrayIndex(nextSlotArrayInd);
                }
                // If we reach end of free space
                else if (i == intBuffer.get(4)) {
                    intBuffer.put(4,i+lengthOfDelete);
                    break;
                }
            }
        }
        // Using Bool to Return True or False
        return isFound;
    
        }

    private int nextSlotArrayIndex(int index) {
        int count_of_zeros = 0;
        int return_value = 0;
        for (int i = index + 1; i < intBuffer.get(0) + count_of_zeros + 5; i++) {
            if (intBuffer.get(i) != 0) {
                return_value = i;
                break;
            }
            count_of_zeros++;
        }
        return return_value;
    }

    /**
     * Returns RID of first record on page. Remember that some slots may be
     * empty, so you should skip over these.
     * @return the RID of the first record on the page. Returns null
     * if the page is empty.
     */
    /** Assuming that we store 0 in slot when rid is deleted **/
    public RID firstRecord()
    {
        RID rid;
        int pageId = 0;
        int slotNum = 0;
        int count_of_zeros = 0;
        if ( intBuffer.get(0) > 0 ) {
            for (int i = 5; i <= intBuffer.get(0) + 4 + count_of_zeros; i++) {
                if ( intBuffer.get(i) != 0 ) {
                    pageId = getPageId();
                    slotNum = intBuffer.get(i);
                    break;
                } else if (intBuffer.get(i) == 0 ) {
                    count_of_zeros++;
                }
            }
            rid = new RID(pageId, slotNum);
            return rid;
        }
        return null;
    }


    /**
     * Returns RID of next record on the page, where "next on the page" means
     * "next in the slot array after the rid passed in." Remember that some
     * slots may be empty, so you should skip over these.
     * @param curRid an RID
     * @return the RID immediately following curRid. Returns null if
     * curRID is the last record on the page.
     * @throws BadPageIdException if the page id within curRid is
     * invalid
     * @throws BadSlotIdException if the slot id within curRid is invalid
    */
    public RID nextRecord(RID curRid)
    {
        RID rid;
        int rid_index = 0;
        int pageId = 0;
        int slotNum = 0;
        int count_of_zeros = 0;
        
        if ( curRid.pageId != getPageId() ) {
            throw new BadPageIdException();
        } else if ( curRid.slotNum > data.length - 1 ) {
            throw new BadSlotIdException();
        } else {
            for (int i = 5; i <= intBuffer.get(0) + 4 + count_of_zeros; i++){
                if (intBuffer.get(i) == 0) {
                    count_of_zeros++;
                }
                else if (intBuffer.get(i) == curRid.slotNum){
                    rid_index = i;
                    break;
                }
            }
            if ( rid_index == intBuffer.get(0) + 4 ){
                return null;
            } else { 
                for (int j = rid_index + 1; j < intBuffer.get(0) + 5; j++){
                    if ( intBuffer.get(j) != 0 ) {
                        pageId = getPageId();
                        slotNum = intBuffer.get(j);
                        break;
                    }
                }
            }
            rid = new RID(pageId, slotNum);
            return rid;
        }
    }

    /**
     * Returns the record associated with an RID.
     * @param rid the rid of interest
     * @return a byte array containing a copy of the record. The array
     * has precisely the length of the record (there is no padded space).
     * @throws BadPageIdException if the page id within curRid is
     * invalid
     * @throws BadSlotIdException if the slot id within curRid is invalid
    */
    public byte[] getRecord(RID rid)
    {
        int length = 0;
        boolean found = false;
        if ( rid.pageId != getPageId() ) {
            System.out.println("the rid.pageId is " + Integer.toString(rid.pageId)+" the getPageId is "+ Integer.toString(getPageId()));
            throw new BadPageIdException();
        } else if ( rid.slotNum > data.length - 1 ) {
            throw new BadSlotIdException();
        } else {
            for ( int i = 5; i <= intBuffer.get(0) + 4; i++ ) {
                if (rid.slotNum == intBuffer.get(i)) {
                    found = true;
                    RID temp = new RID(getPageId(),intBuffer.get(i));
                    if ( nextRecord(temp) != null ) {
                        length = rid.slotNum - nextRecord(temp).slotNum;
                    } else {
                        for (int x = i-1; x > 4; x--) {
                            if (intBuffer.get(x) != 0) {
                                length = intBuffer.get(x) - intBuffer.get(i);
                                break;
                            }
                        }

                    }
                   break;
                }
            }
        }
        
        byte[] temparray = new byte[length];
        int temp_index = 0;
        for ( int i = rid.slotNum; i < rid.slotNum + length; i++ ) {
            temparray[temp_index] = data[i];
            temp_index++;
        }
        
        if (found) { return temparray; }
        else { return null; }
    }

    /**
     * Whether or not the page is empty.
     * @return true if the page is empty, false otherwise.
     */
    public boolean empty()
    {
        System.out.println(data.length);
        if (getAvailableSpace() + headerSize == data.length) { return true; }
        else { return false; }
    }
}
