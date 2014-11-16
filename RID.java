/**
 * Record identifier. Identifies a record id within a heap file. Contains a page id and a slot number.
 * @author Dave Musicant, with considerable material reused from the
 * UW-Madison Minibase project
 */
public class RID
{
    /**
     * Page identification number.
     */
    public int pageId;

    /**
     * Slot number with a heap file page.
     */
    public int slotNum;

    /**
     * Constructor.
     * @param pageId the page id.
     * @param slotNum the slot number.
     */
    public RID(int pageId, int slotNum)
    {
        this.pageId = pageId;
        this.slotNum = slotNum;
    }
}
