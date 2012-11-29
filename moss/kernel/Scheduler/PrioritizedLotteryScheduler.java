package moss.kernel.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import moss.kernel.MProcess;
import moss.kernel.SchedulerBase;

public class PrioritizedLotteryScheduler extends SchedulerBase{
	//Constructor
			public PrioritizedLotteryScheduler() {
				m_listLotteryProcess = new ArrayList<IPrioritizedLotteryProcess>();
				m_listTicketsInUse = new ArrayList<Integer>();
			}
			
			//Private Methods
			@Override
			public Boolean AddProcess(MProcess process) {
				if (process != null){
					IPrioritizedLotteryProcess lotteryProcess = (IPrioritizedLotteryProcess)process;
					List<Integer> tickets = GetTickets(lotteryProcess);
					
					lotteryProcess.SetTickets(tickets);
					m_listTicketsInUse.addAll(tickets);
					m_listLotteryProcess.add((IPrioritizedLotteryProcess)process);
					return true;
				}
			
				return false;
			}

			@Override
			public Boolean RemoveProcess(MProcess process) {
				if (process != null) {
					if (m_listLotteryProcess.contains(process)) {
						m_listLotteryProcess.remove(process);
						IPrioritizedLotteryProcess lotteryProcess = (IPrioritizedLotteryProcess)process;
						m_listTicketsInUse.removeAll(lotteryProcess.GetTickets());
						return true;
					}
				}
				return false;
			}

			@Override
			public MProcess GetNextProcess() {
				Random randNumber = new Random();
				int n = m_listTicketsInUse.size();	
				int index = randNumber.nextInt(n);
				
				int ticket = m_listTicketsInUse.get(index);
				
				for (int i=0; i<m_listLotteryProcess.size(); i++)
				{
					IPrioritizedLotteryProcess lotteryProcess = m_listLotteryProcess.get(i);
					if (lotteryProcess.GetTickets().contains(ticket))
					{
						RemoveProcess((MProcess)lotteryProcess);
						return (MProcess)lotteryProcess;
					}
				}

				return null;
			}
			
			@Override
			public Boolean IsProcessAvailable() {
				return m_listLotteryProcess.size() > 0;
			}
			
			//Private Methods
			private List<Integer> GetTickets(IPrioritizedLotteryProcess process) {
				List<Integer> retValue = new ArrayList<Integer>();
				ProcessPriorityEnum priority = process.GetPriority();
				int ticketsToAssign = 0;
				
				switch(priority) {
				case Low:
					ticketsToAssign = 1;
					break;
				case Medium: 
					ticketsToAssign = 2;
					break;
				case High:
					ticketsToAssign = 3;
					break;
				}
				
				for (int i=0; i<ticketsToAssign; i++){
					retValue.add(GetTicketNumber());
				}
				
				return retValue;
			}
			
			private int GetTicketNumber()
			{
				Random randNumber = new Random();
				int ticket;
				do {
					ticket = randNumber.nextInt(TICKETS);
				} while (m_listTicketsInUse.contains(ticket));
				
				return ticket;
			}
			
			//Private Fields
			private List<IPrioritizedLotteryProcess> m_listLotteryProcess;
			private List<Integer> m_listTicketsInUse;
			private final int TICKETS = Integer.MAX_VALUE;
}
