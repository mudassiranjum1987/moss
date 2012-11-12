package moss.memory;
import java.awt.*; 
import javax.swing.*; 
import java.awt.geom.*; 

public class MEMORY {
	
/* Assume 64 bit address space 
Intel E8000 Series Core Duo 2 has 6MB or 50,331,648 bits in L2 Cache.
This means 786432 addresses. 
Simulate memory in L2 Cache in bits using the 786432 addresses.
Represent each address as a row, each having 64 bits.
*/
String[][] L2CacheArray= new String[786432][64];

public void initializeMemory(){
	
	for (int i=0;i<L2CacheArray.length;i=i+1){
		for (int j=0;j<L2CacheArray[1].length;j=j+1){
			L2CacheArray[i][j]="?"; //Question mark indicates that no process is running.
		}
	}
}

public void exampleMemoryPopulate(){
	
	for (int i=0;i<L2CacheArray.length;i=i+1){
		for (int j=0;j<L2CacheArray[1].length;j=j+1){
			
			if(i<300 & j<100){
				L2CacheArray[i][j]="A"; //Set Equal To Process Name (simulates where process A lives in cache)
			}
			
			if(i>400 |j>200){
				L2CacheArray[i][j]="B";//Set Equal To Another Hypothetical Process Name
			}
		}
	}
	
}

}
