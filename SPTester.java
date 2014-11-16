import java.io.*;
import java.util.*;

public class SPTester
{
    public static interface Testable
    {
        void test() throws Exception;
    }
    
    public static class TestFailedException extends RuntimeException
    {
        public TestFailedException(String explanation)
        {
            super(explanation);
        }
    }

    public static class Test1 implements Testable
    {
        public void test() throws Exception
        {
            SlottedPage sp = new SlottedPage(new Page());
            sp.init();
            
            System.out.println("--- Test 1: Page Initialization Checks ---");
            sp.setPageId(7);
            sp.setNextPageId(8);
            //            sp.setPrevPageId(SlottedPage.INVALID_PAGE);
            
            System.out.println
                ("Current Page No.: " + sp.getPageId() + ", " +
                 "Next Page Id: " + sp.getNextPageId() + ", " +
                 "Prev Page Id: " + sp.getPrevPageId() + ", " +
                 "Available Space: " + sp.getAvailableSpace());
        
            if (!sp.empty())
                throw new TestFailedException("Page should be empty.");

            System.out.println("Page Empty as expected.");
            sp.dumpPage();

        }
    }


    public static class Test2 implements Testable
    {
        public void test() throws Exception
        {
            int buffSize = 20;
            int limit = 20;
            byte[] tmpBuf = new byte[buffSize];

            SlottedPage sp = new SlottedPage(new Page());
            sp.init();
            sp.setPageId(7);
            sp.setNextPageId(8);
//            sp.setPrevPageId(SlottedPage.INVALID_PAGE);

            System.out.println("--- Test 2: Insert and traversal of " +
                               "records ---");
            for (int i=0; i < limit; i++)
            {
                RID rid = sp.insertRecord(tmpBuf);
                System.out.println("Inserted record, RID " + rid.pageId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }

            if (sp.empty())
                throw new TestFailedException("The page cannot be empty");
            
            RID rid = sp.firstRecord();
            while (rid != null)
            {
                tmpBuf = sp.getRecord(rid); 
                System.out.println("Retrieved record, RID " + rid.pageId +
                                   ", " + rid.slotNum);
                rid = sp.nextRecord(rid);
            }
        }
    }


    public static boolean runTest(Testable testObj)
    {
        boolean success = true;
        try
        {
            testObj.test();
        }
        catch (Exception e)
        {
            success = false;
            e.printStackTrace();
        }

        return success;
    }


    public static void main(String[] args)
    {
        System.out.println("Running page tests.");

         runTest(new Test1());
         runTest(new Test2());
    }
}
