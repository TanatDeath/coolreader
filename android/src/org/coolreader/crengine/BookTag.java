package org.coolreader.crengine;

public class BookTag {

	private Long id;
	public String name;
	public String title;
	public Long bookCnt;
	public Long bookCntHier;
	public boolean isSelected;

	public BookTag(String name, String title) {
		this.name = name;
		this.title = title;
	}

	public BookTag(BookTag v)
	{
		id=v.id;
		name=v.name;
		title=v.title;
		bookCnt=v.bookCnt;
		bookCntHier=v.bookCntHier;
		isSelected = false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((bookCnt == null) ? 0 : bookCnt.hashCode());
		result = prime * result + ((bookCntHier == null) ? 0 : bookCntHier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BookTag other = (BookTag) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "BookTag[t=" + name + ", bookCnt=" + bookCnt + ", bookCntHier=" + bookCntHier + "]";
	}

}
