package moss.Time;

import java.util.Random;

public class MProcessTiming{
	
	/* 
	 * MProcessSchedule is generated before the process begins running.
	 * Each row represents a section of time that the process is running on CPU.
	 * The time between the last value in a row and the first value in the next row
	 * represents the amount of time that the process is doing I/O. ~djb
	 */
	
	//Max Process Schedule Time (arbitrarily choose the maxProcessScheduleTime).
	static double maxProcessScheduleTime=200;
	static double maxSegmentLength=20;
	static int maxPossibleProcesses=20;
	static int maxPossibleCPURuns=15;
	
	//Somewhat arbitrarily choose maxPossibleCPURuns segments that would need to run on CPU
	//for any given process. Also arbitrarily chose a max of maxPossibleProcesses processes
	//that would be running at any given time.
	static private double[][][] MProcessSchedules= new double[maxPossibleProcesses][maxPossibleCPURuns][2]; 
	
	//A string of process names that correspond to the first index of MProcessSchedules
	//such that each of maxPossibleProcesses possible processes has maxPossibleCPURuns segments of time that it is running
	//in the CPU.
	static private int[] MProcessScheduleMapping=new int[maxPossibleProcesses];
	
	/*
	 * currentGlobalTime is initialized to 0 and increments when any process advances
	 * by some number of time.
	 * 
	 */
	static private double currentGlobalTime=0;
	
	/* 
	 * processTime represents how much a given process has progressed to completion.
	 * processTime does not increment if a given process is stuck in I/O or another 
	 * process is running on the CPU such that it is possible that real time is still
	 * incrementing while the process time is stagnant.
	 */
	static private double[] processTime=new double[20]; 
	
	//Initialize a single MProcess (for use when a Process has completed) or when initializing the 
	//array that makes up the schedules for all the processes.
	static private void initializeMProcessSchedule(int processIndex){
		
		processTime[processIndex]=0;
		
		MProcessScheduleMapping[processIndex]=-1;
		
		for(int j=0;j<MProcessSchedules[0].length;j=j+1){
			for(int k=0;k<MProcessSchedules[1][1].length;k=k+1){
				
				MProcessSchedules[processIndex][j][k]=-1;
				
			}
		}
		
		
	}
	
	//Initialize the schedules for all of the processes.
	static public void initializeMProcessSchedules(){
		//Initialize all MProcessScheduleMapping value to "?". Assumption is that
		//no process name is called "?".
		for (int i=0;i<MProcessScheduleMapping.length;i=i+1){
			
			initializeMProcessSchedule(i);
			
		}
		
	}
	
	
	/* 
	 * randomProcessScheduleGeneration: For adding a process with some "processName" and a generating a random schedule for it.
	 */
	
	static public void randomProcessScheduleGeneration(int processID){
		
		int processSlot=0;
		Random rnd = new Random();
		
		
		//totalProcessTime is the time that it takes the process to complete
		double totalProcessTime=rnd.nextDouble()*maxProcessScheduleTime;
		
		//Find process slot that is not being used
		while(MProcessScheduleMapping[processSlot]!=-1){
			processSlot=processSlot+1;	
		}
		
		//Place the name of the process inside the schedule mapping.
		MProcessScheduleMapping[processSlot]=processID;
		
		for(int i=0;i<MProcessSchedules[0].length;i=i+1){
			
			if(i==0){
				MProcessSchedules[processSlot][i][0]=rnd.nextDouble()*5;
				//Assume there could be up to a 5s I/O wait in beginning of process
			}else{ 
				if(MProcessSchedules[processSlot][i-1][1]!=totalProcessTime){
					MProcessSchedules[processSlot][i][0]=Math.min(MProcessSchedules[processSlot][i-1][1]+rnd.nextDouble()*maxSegmentLength,totalProcessTime);
				}else{
					MProcessSchedules[processSlot][i][0]=-1; //leave section at end or actual schedule with -1s to indicate schedule end.
				}
			}
			
			if(MProcessSchedules[processSlot][i][0]!=-1){
				MProcessSchedules[processSlot][i][1]=Math.min(MProcessSchedules[processSlot][i-1][1]+rnd.nextDouble()*maxSegmentLength,totalProcessTime);
			}else{
				MProcessSchedules[processSlot][i][1]=-1;
			}
		}
		
	}
	
/*
 * addCPUTimeSlotOnProcess: Writes a new CPU time slot to the process's schedule.
 * returns an error if the desired time slot does not fall after the rest of the schedule.
 * (so a constraint when using the method is that schedule time slots need to be added 
 * sequentially). 
 */	
static public void addCPUTimeSlotOnProcess(int processID,int startTime,int endTime){
		
		int processSlot=0;
		Random rnd = new Random();
		
		//If the process ID hasn't been added to the schedule mapping...
		if(findScheduleNum(processID)==-1){
			//Find process slot that is not being used
			while(MProcessScheduleMapping[processSlot]!=-1){
				processSlot=processSlot+1;	
			}
			
			//Then place the name of the process inside the schedule mapping.
			MProcessScheduleMapping[processSlot]=processID;
		}else{
			
			processSlot=findScheduleNum(processID);
		}
		
		
		boolean processScheduleEndFound=false;
		
		//Iterate through all of the possible time slots
		for(int i=0;i<MProcessSchedules[1].length;i=i+1){
			
			//Once the end of the process is found...
			if((MProcessSchedules[processSlot][i][0]==-1)&&!processScheduleEndFound){
				processScheduleEndFound=true;
				
				//...check to make sure  that the startTime for the schedule segment
				//being added is greater than the current startTime.
				//If that's the case, then add the schedule segment.
				
				if(i==0){
					MProcessSchedules[processSlot][i][0]=startTime;
					MProcessSchedules[processSlot][i][1]=endTime;
					return;
				}
				
				if(startTime>MProcessSchedules[processSlot][i-1][1]){
					MProcessSchedules[processSlot][i][0]=startTime;
					MProcessSchedules[processSlot][i][1]=endTime;
					return;
				}
				
			}
		}
		
	}

	static public double findProcessTime(int processID){
		int scheduleNum=findScheduleNum(processID);
		if (scheduleNum!=-1){
			return processTime[scheduleNum];
		}
		return -1;//return -1 if process doesn't exist..
	}
	
	static private int findScheduleNum(int processID){
		int currProcess=0;
		boolean foundProcessInMap=false;
			while(!foundProcessInMap && currProcess<maxPossibleProcesses){
				
				if(MProcessScheduleMapping[currProcess]==processID)
					return currProcess; //If process is found return its index
				foundProcessInMap=MProcessScheduleMapping[currProcess]==processID;
				currProcess=currProcess+1;
			}

			return -1;//Otherwise return a -1 to indicate that the process' schedule was not found.
	}
	
	//advanceProcess is called in order to advance some process that has
	//been chosen to run on the CPU.
	static public double advanceProcess(int processID,double advanceTimeAmount){
		double globalTimeAdvance=0;
		int processNum=findScheduleNum(processID);
		int searchNextProcessTimeIndex=0;
		boolean processTimeLargerThanCurrentScheduleIndex=processTime[processNum]>=MProcessSchedules[processNum][searchNextProcessTimeIndex][1];
		while (processTimeLargerThanCurrentScheduleIndex&&searchNextProcessTimeIndex<maxPossibleCPURuns){
			searchNextProcessTimeIndex=searchNextProcessTimeIndex+1;
			processTimeLargerThanCurrentScheduleIndex=(processTime[processNum]>=MProcessSchedules[processNum][searchNextProcessTimeIndex][1])
					&&MProcessSchedules[processNum][searchNextProcessTimeIndex][1]!=-1;
		}
		globalTimeAdvance=Math.max(Math.min(MProcessSchedules[processNum][searchNextProcessTimeIndex][1]-processTime[processNum],advanceTimeAmount),0);
		
		//advance the global time for all processes by the amount that 'processNum' ran on CPU.
		currentGlobalTime=currentGlobalTime+globalTimeAdvance;
		
		processTime[processNum]=processTime[processNum]+globalTimeAdvance;
		
		//If other processes were doing I/O while 'processNum' was being advanced, 
		//advance those processes to later processTime states.
		advanceAllProcessesDoingIO(globalTimeAdvance,processNum);
		
		if(MProcessSchedules[processNum][searchNextProcessTimeIndex][0]==-1){
			//Process has finished executing entirely, initialize its spot so that it doesn't run again
			//and new process can take spot.
			
			initializeMProcessSchedule(processNum);
			return -1;//return -1 when process ends.
			
		}
		
		return globalTimeAdvance;
	}
	
	
	//getGlobalTime returns the currentGlobalTime
	static public double getGlobalTime(){
		return currentGlobalTime;
	}
	
	//reset currentGlobalTime to 0. Useful at the end of unit tests to keep global time unit
	//test specific
	static public void resetGlobalTime(){
		currentGlobalTime=0;
		return;
	}
	
	//Advances all of the processes doing I/O by some amount of time (typically while some other process is running on CPU)
	static private void advanceAllProcessesDoingIO(double TimeToAdvanceBy,int processNotToAdvance){
		int searchNextProcessTimeIndex=0;
		
		for(int i=0;i<MProcessSchedules.length;i=i+1){
			//Don't advance process because it is running on CPU during this time and can't do I/O.
			//Also don't advance processes who's first row contains -1. That indicates that this is
			//a blank spot.
			if(i!=processNotToAdvance&&MProcessSchedules[i][0][1]!=-1){
				
			searchNextProcessTimeIndex=0;
			while (processTime[i]>=MProcessSchedules[i][searchNextProcessTimeIndex][1] && MProcessSchedules[i][searchNextProcessTimeIndex+1][1]!=-1){
				searchNextProcessTimeIndex=searchNextProcessTimeIndex+1;
			}
			processTime[i]=Math.min(Math.max(MProcessSchedules[i][searchNextProcessTimeIndex][0],processTime[i]),
					processTime[i]+TimeToAdvanceBy);
			
			}
		}	
	}
	
	static public int[] returnAllProcessesThatCanRunOnCPU(){
		int[] processesReadyToRun=new int[maxPossibleProcesses];
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