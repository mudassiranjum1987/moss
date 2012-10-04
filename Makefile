
JAVAC=javac
JAVAC_OPTS=

MOSS_CORE_CLASSES=moss/kernel/CREWLock.class \
		moss/kernel/MKernelProcess.class \
		moss/kernel/MProcessor.class \
		moss/kernel/Semaphore.class \
		moss/kernel/MInitTask.class \
		moss/kernel/MKernel.class \
		moss/kernel/MProcess.class \
		moss/kernel/MConfig.class \
		moss/kernel/MWaitQueue.class \
		moss/kernel/MTimer.class \
		moss/kernel/MExec.class \
		moss/kernel/MLog.class \
		moss/kernel/MModules.class \
		moss/drivers/MJavaConsole.class \
		moss/drivers/MRamdisk.class \
		moss/drivers/MDevices.class \
		moss/fs/MFileOps.class \
		moss/fs/MFile.class \
		moss/fs/MFileSystem.class \
		moss/fs/MInode.class \
		moss/fs/MDirOps.class \
		moss/fs/MDirEnt.class \
		moss/fs/MFSOps.class \
		moss/fs/MObjFS.class \
		moss/fs/MHostFS.class \
		moss/fs/MDevFS.class \
		moss/fs/MProcFS.class \
		moss/net/MSocket.class \
		moss/net/MSocketAddr.class \
		moss/ipc/MPipe.class \
		moss/ipc/MMailBox.class \
		moss/ipc/MSemaphore.class \
		moss/ipc/MNamedMsgQ.class \
		moss/user/MPosixIf.class \
		moss/user/MUserProcess.class \
		moss/user/MSignal.class \
		moss/user/MSystem.class \
		moss/user/MStdLib.class \
		moss/MiniOSSim.class \
		moss/BlueMOSS.class

MOSS_MODULES=UHelloWorld UConsole UPipeTest UPipeTest2 UProcList \
		UTimerTest UMailRecv UMailSend USemTest USemTest2 \
		ULs UMkdir UCat UWfln ULoadModule UMount UUMount \
		UKLog UKill UCopy UKeyTest UUnlink UBusyLoop \
		\
		KTestMod KWinSys KLogSvr


.PHONY: default
default: all

.SUFFIXES: .java

%.class:	%.java
		$(JAVAC) $(JAVAC_OPTS) $<

moss/modules/%.class:	moss/modules/%.java
			$(JAVAC) $(JAVAC_OPTS) $<


.PHONY: modules
modules:	$(patsubst %, moss/modules/%.class, $(MOSS_MODULES))
		./mkinventory moss/modules

.PHONY: clean
clean:
		find . -name \*.class -exec rm \{\} \;
		find . -name '*.pk[h-z]' -exec rm \{\} \;

.PHONY: all
all:		$(MOSS_CORE_CLASSES) modules

