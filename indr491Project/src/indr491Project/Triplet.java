package indr491Project;

public class Triplet {

	private Integer boutiqueID;
	private String batchID;
	private Integer numOfItems;

	public Triplet(Integer boutiqueID, String batchID, Integer numOfItems) {
		this.boutiqueID = boutiqueID;
		this.batchID = batchID;
		this.numOfItems = numOfItems;
	}

	public Integer getboutiqueID() {
		return boutiqueID;
	}

	public void setboutiqueID(Integer boutiqueID) {
		this.boutiqueID = boutiqueID;
	}

	public String getbatchID() {
		return batchID;
	}

	public void setbatchID(String batchID) {
		this.batchID = batchID;
	}

	public Integer getnumOfItems() {
		return numOfItems;
	}

	public void setnumOfItems(Integer numOfItems) {
		this.numOfItems = numOfItems;
	}
}
