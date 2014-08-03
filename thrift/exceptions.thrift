namespace java de.completionary.proxy.thrift.services.exceptions

exception IndexUnknownException {
	1: string message;
}

exception InvalidIndexNameException {
	1: string message;
}

exception ServerDownException {
	1: string message;
}