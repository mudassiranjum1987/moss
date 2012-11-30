package moss.modules;

import moss.kernel.Scheduler.ProcessPriorityEnum;

public class UUnitTest8 extends UnitTestPrioritiedLotteryBase {

	@Override
	protected int getNumberOfProcesses() {
		return 3;
	}

	@Override
	protected String[] getProcessNameList() {
		return new String[] { "ProcessA", "ProcessB", "ProcessC" };
	}

	@Override
	protected ProcessPriorityEnum[] getProcessPriorityList() {
		return new ProcessPriorityEnum[] { ProcessPriorityEnum.Low, ProcessPriorityEnum.Medium, ProcessPriorityEnum.High };
	}
}
