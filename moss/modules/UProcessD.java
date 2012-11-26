package moss.modules;

import moss.user.*;
import moss.Time.*;

public class UProcessD implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		double quantum=15; //Probably would actually set this inside schedule... this is just basic example.
		double globalTimeAdvanceFromLastProcessRun=0;

		while(globalTimeAdvanceFromLastProcessRun!=-1){
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessD Running On CPU\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessD has PID:"+MPosixIf.getpid()+"\n");
		
			if(MProcessTiming.findProcessTime(MPosixIf.getpid())==-1){
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),0,13);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),45,50);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),52,67);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),69,80);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),85,90);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),92,99);
			}
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessD starts at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			
			globalTimeAdvanceFromLastProcessRun=MProcessTiming.advanceProcess(MPosixIf.getpid(), quantum);
			
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessD ends at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
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