package wizard.skunk.substation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wizard.skunk.parser.domain.Cubicle;
import wizard.skunk.parser.domain.CubicleUnit;
import wizard.skunk.parser.domain.OpticalFiber;
import wizard.skunk.parser.domain.Region;
import wizard.skunk.persistence.substation.domain.*;
import wizard.skunk.persistence.substation.service.BoardService;
import wizard.skunk.persistence.substation.service.CabinetService;
import wizard.skunk.persistence.substation.service.DeviceService;
import wizard.skunk.persistence.substation.service.NetworkCableService;

import java.util.*;

/*
 * SPD:站级物理回路描述(substation physical circuit description)
 */

@Service
public class SPDService {
	private CabinetService cabinetService;
	private DeviceService deviceService;
	private BoardService boardService;
	private NetworkCableService networkCableService;

	private List<Device> devices = new ArrayList<>();
	// <Device.Board.FormatedName, Port>
	private final Map<String, Port> portMap = new HashMap<>();

	@Autowired
	public void setCabinetService(CabinetService cabinetService) {
		this.cabinetService = cabinetService;
	}

	@Autowired
	public void setDeviceService(DeviceService deviceService) {
		this.deviceService = deviceService;
	}

	@Autowired
	public void setBoardService(BoardService boardService) {
		this.boardService = boardService;
	}

	@Autowired
	public void setNetworkCableService(NetworkCableService networkCableService) {
		this.networkCableService = networkCableService;
	}

	private Device findDeviceByName(String name){
		for(Device device : devices){
			if(device.getName().equals(name)){
				return device;
			}
		}
		return null;
	}

	public void persistRegion(List<Region> regions){
		cabinetService.deleteAllDistrict();

		devices.clear();
		devices = deviceService.find();

		for(Region region : regions) {
			District district = new District();
			district.setName(region.getName());
			district.setDescription(region.getDescription());

			cabinetService.saveDistrict(district);

			for(Cubicle cubicle : region.getCubicleList()) {
				Cabinet cabinet = new Cabinet();
				cabinet.setName(cubicle.getName());
				cabinet.setDescription(cubicle.getDescription());
				cabinet.setDistrict(district);

				cabinetService.saveCabinet(cabinet);

				for(CubicleUnit cubicleUnit : cubicle.getCubicleUnits()) {
					Device device = deviceService.findByName(cubicleUnit.getDevice());
					if(device == null){
						if(cubicleUnit.getClassName().equals("TSC") || cubicleUnit.getClassName().equals("ODF")) {
							device = new Device();
							device.setName(cubicleUnit.getDevice());
							device.setDescription(cubicleUnit.getDescription());
							device.setManufacturer(cubicleUnit.getManufacturer());
							device.setType(cubicleUnit.getType());
							device.setCategory(8);

							devices.add(device);
						} else {
							continue;
						}
					}
					device.setCabinet(cabinet);
					device.setCabinetUnit(cubicleUnit.getName());

					deviceService.save(device);

					boardService.deleteBoardByDevice(device);
					for(wizard.skunk.parser.domain.Board board : cubicleUnit.getBoardList()) {
						Board boardEntity = new Board();
						boardEntity.setSlot(board.getSlot());
						boardEntity.setDevice(device);
						boardEntity.setDescription(board.getDescription());
						boardEntity.setManufacturer(board.getManufacturer());
						boardEntity.setType(board.getType());

						boardService.saveBoard(boardEntity);

						for(wizard.skunk.parser.domain.Port port : board.getPortList()) {
							Port portEntity = new Port();
							portEntity.setNo(port.getNo());
							portEntity.setDescription(port.getDescription());
							Port.Direction direction = Port.Direction.fromName(port.getDirection());
							portEntity.setDirection(direction.value);
							portEntity.setIcdPort(port.getIcdPort());
							portEntity.setFormatedName(port.getFormatedName());
							portEntity.setLightReference(port.getLightReference());
							portEntity.setBoard(boardEntity);

							portMap.put(device.getName() + "." + boardEntity.getSlot() + "." + portEntity.getFormatedName(), portEntity);
							boardService.savePort(portEntity);
						}
					}
				}
			}
		}
	}

	public void persistOpticalFiber(List<OpticalFiber> opticalFibers) {
		networkCableService.deleteAll();

		Set<Port> ports = new HashSet<>();
		Map<Port, OpticalFiber> portOpticalFiberMap = new HashMap<>();
		List<NetworkCable> cables = new ArrayList<>();
		for (OpticalFiber opticalFiber : opticalFibers) {
			NetworkCable cable = new NetworkCable();
			cable.setName(opticalFiber.getName());
			cable.setNo(opticalFiber.getNo());

			Device deviceA = findDeviceByName(opticalFiber.getIedA());
			cable.setDeviceAEntity(deviceA);
			if(deviceA.getCategory() == 8){
				opticalFiber.setBoardA("1");
			}
			Device deviceB = findDeviceByName(opticalFiber.getIedB());
			cable.setDeviceBEntity(deviceB);
			if(deviceB.getCategory() == 8){
				opticalFiber.setBoardB("1");
			}

			cable.setDeviceA(opticalFiber.getIedA());
			cable.setBoardA(opticalFiber.getBoardA());
			cable.setPortA(opticalFiber.getPortA());
			cable.setDeviceB(opticalFiber.getIedB());
			cable.setBoardB(opticalFiber.getBoardB());
			cable.setPortB(opticalFiber.getPortB());

			Port portA = portMap.get(opticalFiber.getIedA() + "." + opticalFiber.getBoardA() + "." + opticalFiber.getPortA());
			cable.setPortAEntity(portA);
			if(portA == null){
				System.out.println(opticalFiber + "中端口" + opticalFiber.getIedA() + "." + opticalFiber.getBoardA() + "." + opticalFiber.getPortA() + "不存在");
				continue;
			}
			Port portB = portMap.get(opticalFiber.getIedB() + "." + opticalFiber.getBoardB() + "." + opticalFiber.getPortB());
			cable.setPortBEntity(portB);
			if(portB == null){
				System.out.println(opticalFiber + "中端口" + opticalFiber.getIedB() + "." + opticalFiber.getBoardB() + "." + opticalFiber.getPortB() + "不存在");
				continue;
			}

			if(ports.contains(portA)){
				for(Port port : ports){
					if(port.equals(portA)){
						System.out.println("光纤" + opticalFiber + "的端口" + portA + System.lineSeparator() + "已被光纤" + portOpticalFiberMap.get(port) + "使用");
						break;
					}
				}
				continue;
			}
			if(ports.contains(portB)){
				for(Port port : ports){
					if(port.equals(portB)){
						System.out.println("光纤" + opticalFiber + "的端口" + portB + System.lineSeparator() + "已被光纤" + portOpticalFiberMap.get(port) + "使用");
						break;
					}
				}
				continue;
			}
			ports.add(portA);
			ports.add(portB);

			portOpticalFiberMap.put(portA, opticalFiber);
			portOpticalFiberMap.put(portB, opticalFiber);

			cables.add(cable);
		}

		networkCableService.save(cables);
	}
}
