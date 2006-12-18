SerialPort
{
	classvar allPorts;
	var <dataptr, <semaphore;

	initClass {
		allPorts = Array[];
		UI.registerForShutdown({
			this.closeAll;
		});
	}

	*new {
		| port,
		  baudrate(9600),
		  databits(8),
		  stopbit(true),
		  parity(nil),
		  crtscts(false),
		  xonxoff(false)
		  exclusive(false) |

		^super.new.initSerialPort(
			port,
			exclusive,
			baudrate,
			databits,
			stopbit,
			( even: 1, odd: 2 ).at(parity) ? 0,
			crtscts,
			xonxoff
		)
	}
	initSerialPort { | ... args |
		semaphore = Semaphore(0);
		this.prOpen(*args);
		allPorts = allPorts.add(this);
		CmdPeriod.add(this);
	}

	isOpen {
		^dataptr.notNil
	}
	close {
		if (this.isOpen) {
			this.prClose;
			allPorts.remove(this);
			CmdPeriod.remove(this);
		}
	}
	*closeAll {
		var ports = allPorts;
		allPorts = Array[];
		ports.do(_.close);
	}

	// non-blocking read
	next {
		_SerialPort_Next
		^this.primitiveFailed
	}
	// blocking read
	read {
		var byte;
		while { (byte = this.next).isNil } {
			semaphore.wait;
		};
		^byte
	}
	// always blocks
	put { | byte, timeout=0.005 |
		while { this.prPut(byte).not } {
			timeout.wait;
			timeout = timeout * 2;
		}
	}
	putAll { | bytes, timeout=0.005 |
		bytes.do { |byte|
			this.put(byte, timeout);
		}
	}

	// rename me
	flush {
		_SerialPort_Flush
	}

// 	cmdPeriod {
// 		// remove waiting threads
// 		semaphore.clear;
// 	}

	// PRIMITIVE
	prOpen { | port, baudRate |
		_SerialPort_Open
		^this.primitiveFailed
	}
	prClose {
		_SerialPort_Close
		^this.primitiveFailed
	}
	prPut { | byte |
		_SerialPort_Put
		^this.primitiveFailed
	}
	prDataAvailable {
		// callback
		semaphore.signal;
	}
}
