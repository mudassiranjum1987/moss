package moss.kernel;

import java.util.LinkedList;
import java.util.Queue;

public class FIFOScheduler extends SchedulerBase{

	//Constructor
	public FIFOScheduler() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Boolean AddProcess(MProcess process) {
		if (process != null){
			m_queue.add(process);
			return true;
		}

		return false;
	}

	@Override
	public Boolean RemoveProcess(MProcess process) {
		if (process != null) {
			if (m_queue.contains(process)) {
				m_queue.remove(process);
				return true;
			}
		}
		return false;
	}

	@Override
	public MProcess GetNextProcess() {
		return m_queue.poll();
	}

	@Override
	public Boolean IsProcessAvailable() {
		return m_queue.size() > 0;
	}

	//Private Fields
	Queue<MProcess> m_queue = new LinkedList<MProcess>();
}