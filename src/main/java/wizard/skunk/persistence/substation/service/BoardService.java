package wizard.skunk.persistence.substation.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.persistence.substation.domain.Board;
import wizard.skunk.persistence.substation.domain.Device;
import wizard.skunk.persistence.substation.domain.Port;
import wizard.skunk.persistence.substation.repository.BoardRepository;
import wizard.skunk.persistence.substation.repository.NetworkCableRepository;
import wizard.skunk.persistence.substation.repository.PortRepository;

import java.util.List;

@Service
public class BoardService {
	private final BoardRepository boardRepository;
	private final PortRepository portRepository;
	private NetworkCableRepository cableRepository;

	@Autowired
	public BoardService(BoardRepository repository, PortRepository portRepository) {
		this.boardRepository = repository;
		this.portRepository = portRepository;
	}

	@Autowired
	public void setNetworkCableRepository(NetworkCableRepository cableRepository) {
		this.cableRepository = cableRepository;
	}

	public List<Board> findByDevice(Device device) {
		return (List<Board>) boardRepository.findByDevice(device);
	}

	public List<Port> findPortsByBoard(Board board) {
		return (List<Port>) portRepository.findByBoard(board);
	}

	@Transactional
	public void deleteBoardByDevice(Device device){
		cableRepository.deleteByDeviceAEntityOrDeviceBEntity(device, device);
		portRepository.deleteByDevice(device);
		boardRepository.deleteByDevice(device);
	}

	public void saveBoard(Board board) {
		boardRepository.save(board);
	}

	public void savePort(Port portEntity) {
		portRepository.save(portEntity);
	}
}
