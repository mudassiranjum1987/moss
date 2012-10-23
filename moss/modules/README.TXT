
This directory contains the various MOSS "modules".  These are:
    * user programs
    * kernel modules

User programs have class names beginning with "U" and implement the
MUserProcess interface (in moss/user).  Kernel module names begin
with "K" and implement the MKernelProcess interface (in moss/kernel).

When MOSS starts up, it copies the various .class files from here
into the MOSS internal file-system.  There is a file in the top-level
MOSS directory called "INVENTORY".  This contains mappings from
generated .class files (relative to the MOSS top-level directory) to
files in the MOSS file-system.  For example:

    moss/modules/ULs.class /bin/ls
    
Will make that class available from /bin/ in MOSS.  Any inner-classes
generated (as KWinSys has) must go into /lib/ in MOSS.
Kernel modules (eg KWinSys) go in /modules/.


User Programs
-------------

UConsole (/bin/console)        -- the MOSS console (user-interface) process
UMount (/bin/mount)            -- file-system mount utility
UUMount (/bin/umount)          -- file-system un-mount utility
ULs (/bin/ls)                  -- list files in directory
UMkdir (/bin/mkdir)            -- make directory
UWfln (/bin/wfln)              -- write a line of text to a file
UCat (/bin/cat)                -- concatenate files (used to show file contents)
UHelloWorld (/bin/helloworld)  -- simple "hello world" program
UProcList (/bin/proclist)      -- show MOSS processes
ULoadModule (/bin/loadmodule)  -- utility for loading kernel modules

UTimerTest (/bin/timertest)    -- timer test (non-terminating)
UPipeTest (/bin/pipetest)      -- pipe test
UPipeTest2 (/bin/pipetest2)    -- other half of /bin/pipetest [*]
USemTest (/bin/semtest)        -- semaphore test
USemTest2 (/bin/semtest2)      -- other half of /bin/semtest [*]
UMailRecv (/bin/mailrecv)      -- mailbox test
UMailSend (/bin/mailsend)      -- other half of /bin/mailrecv [*]


[*] Don't run these directly yourself; they're invoked by
    their other-half.


Kernel Modules
--------------

KTestMod (/modules/testmod)    -- simple example kernel module
KWinSys (/modules/winsys)      -- MOSS windowing system kernel module



