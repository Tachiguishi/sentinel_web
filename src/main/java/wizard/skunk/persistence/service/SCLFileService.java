package wizard.skunk.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.domain.SCLFile;
import wizard.skunk.persistence.repository.SCLFileRepository;

import java.io.*;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@Service
public class SCLFileService {
	private final SCLFileRepository sclFileRepository;

	@Autowired
	public SCLFileService(SCLFileRepository sclFileRepository) {
		this.sclFileRepository = sclFileRepository;
	}

	public String findCIDFileName(String identifier){
		return sclFileRepository.findFileNameByCategoryAndIdentifier(SCLFile.Category.CID.ordinal(), identifier);
	}

	public void updateCIDIdentifier(String originalIdentifier, String newIdentifier){
		sclFileRepository.updateIdentifierByCategoryAndIdentifier(SCLFile.Category.CID.ordinal(), originalIdentifier, newIdentifier);
	}

	public void deleteCIDFile(String identifier) {
		sclFileRepository.deleteByCategoryAndIdentifier(SCLFile.Category.CID.ordinal(), identifier);
	}

	public void deleteByIdentifier(String identifier){
		sclFileRepository.deleteByIdentifier(identifier);
	}

	public void saveCIDFile(File file, String identifier) throws IOException {
		SCLFile sclFile = sclFileRepository.findTopByCategoryAndIdentifier(SCLFile.Category.CID.ordinal(), identifier);
		if(sclFile == null){
			sclFile = new SCLFile();
			sclFile.setCategory(SCLFile.Category.CID.ordinal());
			sclFile.setIdentifier(identifier);
		}

		save(file, sclFile);
	}

	private void save(File file, SCLFile sclFile) throws IOException {
		// compress content
		ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream();
		try(FileInputStream fileInputStream = new FileInputStream(file)){
			DeflaterOutputStream outputStream = new DeflaterOutputStream(resultOutputStream);
			byte[] buffer = new byte[10240];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.finish();
		}
		sclFile.setContent(resultOutputStream.toByteArray());
		sclFile.setFileName(file.getName());

		sclFileRepository.save(sclFile);
	}

	public byte[] readCIDFile(String identifier) throws IOException {
		SCLFile sclFile = sclFileRepository.findTopByCategoryAndIdentifier(SCLFile.Category.CID.ordinal(), identifier);
		if(sclFile == null){
			return null;
		}

		return decompressFileContent(sclFile.getContent());
	}

	private byte[] decompressFileContent(byte[] rawContent) throws IOException {
		ByteArrayOutputStream resultOutputStream = new ByteArrayOutputStream();
		try (InflaterInputStream inputStream = new InflaterInputStream(new ByteArrayInputStream(rawContent))){
			byte[] buffer = new byte[10240];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				resultOutputStream.write(buffer, 0, bytesRead);
			}
			resultOutputStream.flush();
		}
		return resultOutputStream.toByteArray();
	}
}
