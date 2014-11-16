JAVAC = javac

SRCS = Page.java RID.java SPTester.java SlottedPage.java

project: $(SRCS)
	$(JAVAC)  $^

%.o : %.c $(HDRS)
	$(JAVAC)  $(CFLAGS) -c $<  -o $@

clean:
	rm *.class

