package moss.modules;

import moss.user.*;
import moss.Time.*;

public class UProcessE implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		double quantum=15; //Probably would actually set this inside schedule... this is just basic example.
		double globalTimeAdvanceFromLastProcessRun=0;

		while(globalTimeAdvanceFromLastProcessRun!=-1){
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessE Running On CPU\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessE has PID:"+MPosixIf.getpid()+"\n");
		
			if(MProcessTiming.findProcessTime(MPosixIf.getpid())==-1){
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),0,12);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),67,75);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),81,85);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),90,150);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),170,180);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),190,200);
			}
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessE starts at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			
			globalTimeAdvanceFromLastProcessRun=MProcessTiming.advanceProcess(MPosixIf.getpid(), quantum);
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessE ends at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
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