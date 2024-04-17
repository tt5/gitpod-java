package raptor.engine.uci.info;

import raptor.engine.uci.UCIInfo;

public class MultiPV implements UCIInfo {
	protected int pvid;

	public MultiPV(String pvid) {
		super();
		this.pvid = Integer.parseInt(pvid);
	}

	public int getId() {
		return pvid;
	}

	public void setId(int pvid) {
		this.pvid = pvid;
	}

  @Override
  public String toString() {
    return "MultiPV [id=" + pvid + "]";
  }

}
