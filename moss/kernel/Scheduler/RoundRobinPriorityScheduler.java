package moss.kernel.Scheduler;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import moss.kernel.MProcess;
import moss.kernel.SchedulerBase;

public class RoundRobinPriorityScheduler extends SchedulerBase implements IPriorityScheduler {

	//Constructor
	public RoundRobinPriorityScheduler() {
		// TODO Auto-generated constructor stub		
	}
		
	@Override
	public Boolean setProcessPriority(int pid, ProcessPriorityEnum priority) {
		Enumeration<Queue<MProcess>> enumProcessQueue = getPriorityToQueueMap().elements();
		
		while (enumProcessQueue.hasMoreElements()) {
			Queue<MProcess> iterateProcessQueue = enumProcessQueue.nextElement();
			
			Iterator<MProcess> iteratorProcess = iterateProcessQueue.iterator();
			while (iteratorProcess.hasNext()) {
				MProcess iterateProcess = iteratorProcess.next();
				if (iterateProcess.pid == pid){
					iterateProcess.priority = priority;
					
					iterateProcessQueue.remove(iterateProcess);
					AddProcess(iterateProcess);
					
					return true;
				}
			}
		}
		
		return false;
	}

	@Override
	public Boolean AddProcess(MProcess process) {
		if (process != null){
			Queue<MProcess> queueSelected = GetProcessQueue(process);
			queueSelected.add(process);
			return true;
		}

		return false;
	}

	@Override
	public Boolean RemoveProcess(MProcess process) {
		if (process != null) {
			Queue<MProcess> queueSelected = GetProcessQueue(process);
			if (queueSelected.contains(process)) {
				queueSelected.remove(process);
				return true;
			}
		}
		return false;
	}

	@Override
	public MProcess GetNextProcess() {
		Enumeration<Integer> enumPriorities = getPriorityToQueueMap().keys();
		int priorityKey = 0;
		
		while (enumPriorities.hasMoreElements()) {
			int iteratePriorityKey = enumPriorities.nextElement();
			if (priorityKey < iteratePriorityKey && getPriorityToQueueMap().get(iteratePriorityKey).size() > 0)
				priorityKey = iteratePriorityKey;
		}
		
		MProcess retValue = getPriorityToQueueMap().get(priorityKey).poll();
		if (retValue.priority.getPriorityValue() > 0)
			retValue.priority.setPriorityValue(retValue.priority.getPriorityValue() - 1);
		return retValue;
	}

	@Override
	public Boolean IsProcessAvailable() {
		
		Enumeration<Queue<MProcess>> enumQueue = getPriorityToQueueMap().elements();
		while (enumQueue.hasMoreElements()) {
			if (enumQueue.nextElement().size() > 0)
				return true;
		}
		
		return false;
	}

	//Private Methods
	private Hashtable<Integer, Queue<MProcess>> getPriorityToQueueMap(){
		return m_priorityToQueueMap;
	}
	private Queue<MProcess> GetProcessQueue(MProcess process) {
		IPriorityProcess priorityProcess = (IPriorityProcess)process;
		int priorityValue = priorityProcess.GetPriority().getPriorityValue();
		
		//If queue already exists then return it
		if (getPriorityToQueueMap().containsKey(priorityValue))
			return getPriorityToQueueMap().get(priorityValue);
		
		//Otherwise create a new queue and return it.
		Queue<MProcess> priorityQueue = new LinkedList<MProcess>();
		getPriorityToQueueMap().put(priorityValue, priorityQueue);
		
		return priorityQueue;
	}
	
	//Private Fields
	Hashtable<Integer, Queue<MProcess>> m_priorityToQueueMap = new Hashtable<Integer, Queue<MProcess>>();
}
