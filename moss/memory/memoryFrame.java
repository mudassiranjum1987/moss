package moss.memory;

import java.awt.Container;

import javax.swing.JFrame;

public class memoryFrame extends JFrame { 
	  public memoryFrame() { 
	     super("Memory Image"); 
	     setSize(1500, 950); 
	     setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
	     MemoryPanel sp = new MemoryPanel(); 
	     MEMORY mem=new MEMORY();
	     
	     sp.setBitSize(2);
	     sp.setNumAddressInPanelColumn(450);
	     sp.setNumAddressInCache(4500);
	     //786432 in intel core duo. Maybe make it so that each 
	     //dot (now bit) represents an address? Otherwise can't fit on screen
	     //easily. ~djb
	     mem.initializeMemory();
	     mem.exampleMemoryPopulate(); //Populate memory with processes to run...
	     sp.initializeProcessColorMapping();
	     sp.setMemoryRepresentation(mem.L2CacheArray);//Send Cache Array to MemoryPanel in order to print to screen.
	     Container content = getContentPane(); 
	     content.add(sp); 
	     setContentPane(content); 
	     setVisible(true); 
	 } 
}

