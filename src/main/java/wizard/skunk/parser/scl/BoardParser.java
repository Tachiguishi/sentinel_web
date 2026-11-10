package wizard.skunk.parser.scl;

import org.w3c.dom.Element;
import wizard.skunk.parser.domain.Board;
import wizard.skunk.parser.domain.Port;
import wizard.skunk.parser.xml.XMLHelper;

import java.util.ArrayList;
import java.util.List;

public class BoardParser {
	public static List<Board> parse(Element parentNode){
		List<Board> result = new ArrayList<>();

		if(parentNode == null){
			return result;
		}

		List<Element> boardNodes = XMLHelper.getElementsByTagName(parentNode, "Board");
		for(Element boardNode : boardNodes){
			String slot = boardNode.getAttribute("slot");
			String description = boardNode.getAttribute("desc");
			String type = boardNode.getAttribute("type");

			Board board = new Board();
			board.setSlot(slot);
			board.setDescription(description);
			board.setType(type);

			parsePort(boardNode, board);

			result.add(board);
		}

		List<Element> boardMapNodes = XMLHelper.getElementsByTagName(parentNode, "BoardMap");
		for(Element boardMapNode : boardMapNodes){
			String slot = boardMapNode.getAttribute("PhysSlot");
			String logicSlot = boardMapNode.getAttribute("LogicSlot");

			for(Board board : result){
				if(board.getSlot().equals(slot)){
					board.setAlias(logicSlot);
				}
			}
		}

		return result;
	}

	private static void parsePort(Element boardNode, Board board){
		List<Element> portNodes = XMLHelper.getElementsByTagName(boardNode, "Port");
		for(Element portNode : portNodes){
			String no = portNode.getAttribute("no");
			String description = portNode.getAttribute("desc");
			String direction = portNode.getAttribute("type");
			String plug = portNode.getAttribute("plug");
			String usage = portNode.getAttribute("usage");
			String lightReference = portNode.getAttribute("scliRef");

			Port port = new Port();
			port.setNo(no);
			port.setDescription(description);
			port.setDirection(direction);
			port.setPlug(plug);
			port.setUsage(usage);
			port.setLightReference(lightReference);

			board.addPort(port);
		}
	}
}
