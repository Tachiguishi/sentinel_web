package wizard.skunk.export;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

public class SyncAction {
	private void serializeInt(byte[] writeBuffer, int v){
		writeBuffer[0] = (byte)(v);
		writeBuffer[1] = (byte)(v >>> 8);
		writeBuffer[2] = (byte)(v >>> 16);
		writeBuffer[3] = (byte)(v >>> 24);
	}

	private void serializeLong(byte[] writeBuffer, long v){
		writeBuffer[0] = (byte)((int)(v));
		writeBuffer[1] = (byte)((int)(v >>> 8));
		writeBuffer[2] = (byte)((int)(v >>> 16));
		writeBuffer[3] = (byte)((int)(v >>> 24));
		writeBuffer[4] = (byte)((int)(v >>> 32));
		writeBuffer[5] = (byte)((int)(v >>> 40));
		writeBuffer[6] = (byte)((int)(v >>> 48));
		writeBuffer[7] = (byte)((int)(v >>> 56));
	}

	public void generateSyncFile(FileOutputStream outputStream, ByteArrayOutputStream dataStream, String substationCode) throws Exception{
		DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
		int messageType = 10008;

		byte[] substation = substationCode.getBytes();

		String fileNameStr = substationCode + ".xml";
		byte[] fileName = fileNameStr.getBytes();

		byte[] fileData = dataStream.toByteArray();
		int messageSize = 169 + fileData.length;
		long fileSize = fileData.length;
		byte fileType = 22;

		byte[] writeBuffer = new byte[8];
		serializeInt(writeBuffer, messageType);
		dataOutputStream.write(writeBuffer, 0, 4);
		serializeInt(writeBuffer, messageSize);
		dataOutputStream.write(writeBuffer, 0, 4);
		for(int i = 0; i < 32; i++){
			if(i < substation.length){
				dataOutputStream.writeByte(substation[i]);
			}
			if(i >= substation.length){
				dataOutputStream.writeByte(0);
			}
		}
		for(int i = 0; i < 128; i++){
			if(i < fileName.length){
				dataOutputStream.writeByte(fileName[i]);
			}
			else{
				dataOutputStream.writeByte(0);
			}
		}
		serializeLong(writeBuffer, fileSize);
		dataOutputStream.write(writeBuffer, 0, 8);
		dataOutputStream.writeByte(fileType);
		dataOutputStream.write(fileData);

		// Ensure all data is written to the file and close the stream
		dataOutputStream.flush();
		dataOutputStream.close();
	}
}
