package moss.memory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Random;

import javax.swing.JPanel;


class MemoryPanel extends JPanel { 

	int bitSize=1;
	int numBitsInAddress=64; //Assume 64 bit architecture
	int numAddressInCache=1;
	int numAddressInPanelColumn=400;
	
	
	//Each Process Will have 3 float numbers associated with it
	//that make up that process' color.
	float[][] HSBprocessColorMapping= new float[786432][3];
	String[] ProcessForColorMapping= new String[786432];
	String[][] MemoryRepresentation=new String[786432][64];
	
	Random rnd = new Random();
	
	public void initializeProcessColorMapping(){
		
		for(int i=0;i<786432;i=i+1){
			
			ProcessForColorMapping[i]="?";
			HSBprocessColorMapping[i][0]=0;
			HSBprocessColorMapping[i][1]=0;
			HSBprocessColorMapping[i][2]=0;//Zero Brightness means color will be black.
			
		}
	}
	public float[] updateColorMap(float[][] HSBprocessColorMapping,String[] ProcessForColorMapping,String newProcessToMap){
		
		int counter=0;
		
		while(newProcessToMap!="?"&((ProcessForColorMapping[counter]!="?")&(ProcessForColorMapping[counter]!=newProcessToMap))){
			counter=counter+1;
		}
		
		
		if (ProcessForColorMapping[counter]=="?"&newProcessToMap!="?"){
			
			ProcessForColorMapping[counter]=newProcessToMap;
			HSBprocessColorMapping[counter][0]=rnd.nextFloat();
			HSBprocessColorMapping[counter][1]=rnd.nextFloat();
			HSBprocessColorMapping[counter][2]=rnd.nextFloat();
			
		}
		
		return HSBprocessColorMapping[counter];
	}
	
	public void setMemoryRepresentation(String[][] newMemoryRepresentation){
		
		MemoryRepresentation=newMemoryRepresentation;
	}
	
	public void setBitSize(int newSize){
		
		bitSize=newSize;
	}
	
	public void setNumBitsInAddress(int newNumBitsInAddress){
		numBitsInAddress=newNumBitsInAddress;
	}
	
	public void setNumAddressInCache(int newNumCacheAddress){
		
		numAddressInCache=newNumCacheAddress;
	}
	
	public void setNumAddressInPanelColumn(int newNumAddressInPanelColumn){
		
		numAddressInPanelColumn=newNumAddressInPanelColumn;
	}
	
	
	
	public void paintComponent(Graphics comp) { 
	     super.paintComponent(comp); 
	     Graphics2D comp2D = (Graphics2D) comp; 
	     
	     float[] tempHSBColorStore=new float[3];
	     tempHSBColorStore[0]=0;
	     tempHSBColorStore[1]=0;
	     tempHSBColorStore[2]=0;
	     
	     //Initialization of Memory Happens Here. Initialize to Black
	     for(int i=0;i<numAddressInCache;i=i+1){
	     	for(int j=0;j<numBitsInAddress;j=j+1){
	     		
	     		if(MemoryRepresentation[i][j]=="?"){
	     			comp2D.setColor(Color.black);
	     		}else{
	     			tempHSBColorStore=updateColorMap(HSBprocessColorMapping,ProcessForColorMapping,MemoryRepresentation[i][j]);
	     			comp2D.setColor(Color.getHSBColor(tempHSBColorStore[0], tempHSBColorStore[1], tempHSBColorStore[2]));
	     		}
	     		
	     		Ellipse2D.Float sign1 = new Ellipse2D.Float(bitSize*j+Math.round((Math.floor(i/numAddressInPanelColumn)))*(numBitsInAddress+10)*bitSize,
	     				bitSize*(i%numAddressInPanelColumn), bitSize, bitSize); 
	     		comp2D.fill(sign1); 
	     	}
	     }

	 } 
	}