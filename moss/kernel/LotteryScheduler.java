package moss.kernel;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class LotteryScheduler extends SchedulerBase {
		//Constructor
		public LotteryScheduler() {
			m_listLotteryProcess = new ArrayList<ILotteryProcess>();
			m_listTicketsInUse = new ArrayList<Integer>();
		}
		
		//Private Methods
		@Override
		public Boolean AddProcess(MProcess process) {
			if (process != null){
				ILotteryProcess lotteryProcess = (ILotteryProcess)process;
				int ticket = GetTicketNumber();
				
				lotteryProcess.SetTicket(ticket);
				m_listTicketsInUse.add(ticket);
				m_listLotteryProcess.add((ILotteryProcess)process);
				return true;
			}
		
			return false;
		}

		@Override
		public Boolean RemoveProcess(MProcess process) {
			if (process != null) {
				if (m_listLotteryProcess.contains(process)) {
					m_listLotteryProcess.remove(process);
					ILotteryProcess lotteryProcess = (ILotteryProcess)process;
					m_listTicketsInUse.remove((Object)lotteryProcess.GetTicket());
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
				ILotteryProcess lotteryProcess = m_listLotteryProcess.get(i);
				if (lotteryProcess.GetTicket() == ticket)
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
		int GetTicketNumber()
		{
			Random randNumber = new Random();
			int ticket;
			do {
				ticket = randNumber.nextInt(TICKETS);
			} while (m_listTicketsInUse.contains(ticket));
			
			return ticket;
		}
		
		//Private Fields
		private List<ILotteryProcess> m_listLotteryProcess;
		private List<Integer> m_listTicketsInUse;
		private final int TICKETS = Integer.MAX_VALUE;
}
