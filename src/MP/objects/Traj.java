package MP.objects;

import java.util.LinkedList;

public class Traj {

	public LinkedList<Integer> list;
	public boolean Ended = false;

	public Traj(int loc) {
		Ended = false;
		list = new LinkedList<Integer>();
		list.add(loc);
	}
}
