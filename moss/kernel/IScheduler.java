package moss.kernel;

public interface IScheduler {
	Boolean AddProcess(MProcess process);
	Boolean RemoveProcess(MProcess process);
	MProcess GetNextProcess();
	Boolean Schedule();	
	Boolean IsProcessAvailable();
}
