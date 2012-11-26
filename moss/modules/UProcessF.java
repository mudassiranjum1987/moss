package moss.modules;

import moss.user.*;
import moss.Time.*;

public class UProcessF implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		double quantum=15; //Probably would actually set this inside schedule... this is just basic example.
		double globalTimeAdvanceFromLastProcessRun=0;

		while(globalTimeAdvanceFromLastProcessRun!=-1){
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessF Running On CPU\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessF has PID:"+MPosixIf.getpid()+"\n");
		
			if(MProcessTiming.findProcessTime(MPosixIf.getpid())==-1){
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),0,5);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),10,15);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),20,25);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),30,35);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),40,45);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),50,55);
			}
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessF starts at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			
			globalTimeAdvanceFromLastProcessRun=MProcessTiming.advanceProcess(MPosixIf.getpid(), quantum);
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessF ends at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			MPosixIf.writestring(MPosixIf.STDOUT, "Global time is:"+MProcessTiming.getGlobalTime()+"\n");
			
			
			MPosixIf.reschedule();
		}
		//MPosixIf.exit (0);
		return 0;
	}
	
	public void signal (int signo, Object sigdata)
	{
		return;
	}
	
}