package Time;

import java.util.Random;

public class MProcessTiming{
	
	/* 
	 * MProcessSchedule is generated before the process begins running.
	 * Each row represents a section of time that the process is running on CPU.
	 * The time between the last value in a row and the first value in the next row
	 * represents the amount of time that the process is doing I/O. ~djb
	 */
	
	//Max Process Schedule Time (arbitrarily choose the maxProcessScheduleTime).
	double maxProcessScheduleTime=200;
	double maxSegmentLength=20;
	int maxPossibleProcesses=20;
	int maxPossibleCPURuns=15;
	
	//Somewhat arbitrarily choose maxPossibleCPURuns segments that would need to run on CPU
	//for any given process. Also arbitrarily chose a max of maxPossibleProcesses processes
	//that would be running at any given time.
	double[][][] MProcessSchedules= new double[maxPossibleProcesses][maxPossibleCPURuns][2]; 
	
	//A string of process names that correspond to the first index of MProcessSchedules
	//such that each of maxPossibleProcesses possible processes has maxPossibleCPURuns segments of time that it is running
	//in the CPU.
	String[] MProcessScheduleMapping=new String[maxPossibleProcesses];
	
	/* 
	 * processTime represents how much a given process has progressed to completion.
	 * processTime does not increment if a given process is stuck in I/O or another 
	 * process is running on the CPU such that it is possible that real time is still
	 * incrementing while the process time is stagnant.
	 */
	double[] processTime=new double[20]; 
	
	//Initialize a single MProcess (for use when a Process has completed) or when initializing the 
	//array that makes up the schedules for all the processes.
	private void initializeMProcessSchedule(int processIndex){
		
		processTime[processIndex]=0;
		
		MProcessScheduleMapping[processIndex]="?";
		
		for(int j=0;j<MProcessSchedules[1].length;j=j+1){
			for(int k=0;k<MProcessSchedules[1][1].length;k=k+1){
				
				MProcessSchedules[processIndex][j][k]=-1;
				
			}
		}
		
		
	}
	
	//Initialize the schedules for all of the processes.
	public void initializeMProcessSchedules(){
		//Initialize all MProcessScheduleMapping value to "?". Assumption is that
		//no process name is called "?".
		for (int i=0;i<MProcessScheduleMapping.length;i=i+1){
			
			initializeMProcessSchedule(i);
			
		}
		
	}
	
	
	/* 
	 * randomProcessScheduleGeneration: For adding a process with some "processName" and a generating a random schedule for it.
	 */
	
	public void randomProcessScheduleGeneration(String processName){
		
		int processSlot=0;
		Random rnd = new Random();
		
		
		//totalProcessTime is the time that it takes the process to complete
		double totalProcessTime=rnd.nextDouble()*maxProcessScheduleTime;
		
		//Find process slot that is not being used
		while(MProcessScheduleMapping[processSlot]!="?"){
			processSlot=processSlot+1;	
		}
		
		//Place the name of the process inside the schedule mapping.
		MProcessScheduleMapping[processSlot]=processName;
		
		for(int i=0;i<MProcessSchedules[1].length;i=i+1){
			
			if(i==0){
				MProcessSchedules[processSlot][i][1]=rnd.nextDouble()*5;
				//Assume there could be up to a 5s I/O wait in beginning of process
			}else{ 
				if(MProcessSchedules[processSlot][i-1][2]!=totalProcessTime){
					MProcessSchedules[processSlot][i][1]=Math.min(MProcessSchedules[processSlot][i-1][2]+rnd.nextDouble()*maxSegmentLength,totalProcessTime);
				}else{
					MProcessSchedules[processSlot][i][1]=-1; //leave section at end or actual schedule with -1s to indicate schedule end.
				}
			}
			
			if(MProcessSchedules[processSlot][i][1]!=-1){
				MProcessSchedules[processSlot][i][2]=Math.min(MProcessSchedules[processSlot][i-1][2]+rnd.nextDouble()*maxSegmentLength,totalProcessTime);
			}else{
				MProcessSchedules[processSlot][i][2]=-1;
			}
		}
		
	}
	
	private int findScheduleNum(String processName){
		int currProcess=0;
			while(MProcessScheduleMapping[currProcess]!=processName){
				currProcess=currProcess+1;
			}
		if(MProcessScheduleMapping[currProcess]==processName)
			
			return currProcess;//If process is found return its index
		else
			return -1;//Otherwise return a -1 to indicate that the process' schedule was not found.
	}
	
	private void deleteProcess(String processName){
		
		initializeMProcessSchedule(findScheduleNum(processName));
		
	}
	
	//advanceProcess is called in order to advance some process that has
	//been chosen to run on the CPU.
	public double advanceProcess(String processName){
		double globalTimeAdvance=0;
		int processNum=findScheduleNum(processName);
		int searchNextProcessTimeIndex=0;
		
		while (processTime[processNum]>MProcessSchedules[processNum][searchNextProcessTimeIndex][2]){
			searchNextProcessTimeIndex=searchNextProcessTimeIndex+1;
		}
		globalTimeAdvance=MProcessSchedules[processNum][searchNextProcessTimeIndex][2]-processTime[processNum];
		processTime[processNum]=MProcessSchedules[processNum][searchNextProcessTimeIndex][2];

		advanceAllProcessesDoingIO(globalTimeAdvance,processNum);
		
		if(MProcessSchedules[processNum][searchNextProcessTimeIndex+1][1]==-1){
			//Process has finished executing entirely, initialize its spot so that it doesn't run again
			//and new process can take spot.
			
			initializeMProcessSchedule(processNum);
			
		}
		
		return globalTimeAdvance;
	}
	
	private void advanceAllProcessesDoingIO(double TimeToAdvanceBy,int processNotToAdvance){
		int searchNextProcessTimeIndex=0;
		
		for(int i=0;i<MProcessSchedules.length;i=i+1){
			if(i!=processNotToAdvance){//Don't advance process because it is running on CPU during this time and can't do I/O.
				
			searchNextProcessTimeIndex=0;
			while (processTime[i]>=MProcessSchedules[i][searchNextProcessTimeIndex][2]){
				searchNextProcessTimeIndex=searchNextProcessTimeIndex+1;
			}
			processTime[i]=Math.min(MProcessSchedules[i][searchNextProcessTimeIndex][1], processTime[i]+TimeToAdvanceBy);
			
			}
		}	
	}
	
	public String[] returnAllProcessesThatCanRunOnCPU(){
		String[] processesReadyToRun=new String[maxPossibleProcesses];
		int countProcessesReadytoRun=0;
		for(int i=0;i<MProcessSchedules.length;i=i+1){
			for(int j=0;j<MProcessSchedules[1].length;j=j+1){
			
				if(processTime[i]==MProcessSchedules[i][j][1]){
					processesReadyToRun[countProcessesReadytoRun]=MProcessScheduleMapping[i];
					countProcessesReadytoRun=countProcessesReadytoRun+1;
				}
				
			}
		}
		
		return processesReadyToRun;
	}
}
