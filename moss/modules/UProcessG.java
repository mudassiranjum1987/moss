package moss.modules;

import moss.user.*;
import moss.Time.*;

public class UProcessG implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		double quantum=15; //Probably would actually set this inside schedule... this is just basic example.
		double globalTimeAdvanceFromLastProcessRun=0;

		while(globalTimeAdvanceFromLastProcessRun!=-1){
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessG Running On CPU\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessG has PID:"+MPosixIf.getpid()+"\n");
		
			if(MProcessTiming.findProcessTime(MPosixIf.getpid())==-1){
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),0,2);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),10,19);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),33,44);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),53,67);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),73,89);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),101,121);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),130,133);
			}
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessG starts at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			
			globalTimeAdvanceFromLastProcessRun=MProcessTiming.advanceProcess(MPosixIf.getpid(), quantum);
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessG ends at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
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