package moss.modules;

import moss.user.*;
import Time.*;

public class UProcessB implements MUserProcess{

	public int main (String argv[], MEnv envp)
	{
		double quantum=15; //Probably would actually set this inside schedule... this is just basic example.
		double globalTimeAdvanceFromLastProcessRun=0;

		while(globalTimeAdvanceFromLastProcessRun!=-1){
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessB Running On CPU\n");
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessB has PID:"+MPosixIf.getpid()+"\n");
		
			if(MProcessTiming.findProcessTime(MPosixIf.getpid())==-1){
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),0,30);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),32,40);
				MProcessTiming.addCPUTimeSlotOnProcess(MPosixIf.getpid(),60,80);
			}
			MPosixIf.writestring (MPosixIf.STDOUT, "ProcessB is at process time:"+MProcessTiming.findProcessTime(MPosixIf.getpid())+"\n");
			
			globalTimeAdvanceFromLastProcessRun=MProcessTiming.advanceProcess(MPosixIf.getpid(), quantum);
			MPosixIf.reschedule();
		}
		MPosixIf.exit (0);
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UHelloWorld signalled with " + signo + "!\n");
	}
	
}

