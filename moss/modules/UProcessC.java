package moss.modules;

import moss.user.*;
import moss.Time.*;

public class UProcessC implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		double quantum=15; //Probably would actually set this inside schedule... this is just basic example.
		double globalTimeAdvanceFromLastProcessRun=0;

		while(globalTimeAdvanceFromLastProcessRun!=-1){
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessC Running On CPU\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessC has PID:"+MPosixIf.getpid()+"\n");
		
			if(MProcessTiming.findProcessTime(MPosixIf.getpid())==-1){
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),0,23);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),55,90);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),100,110);
			}
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessC starts at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			
			globalTimeAdvanceFromLastProcessRun=MProcessTiming.advanceProcess(MPosixIf.getpid(), quantum);
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessC ends at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
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