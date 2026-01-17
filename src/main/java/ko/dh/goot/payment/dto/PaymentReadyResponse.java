package ko.dh.goot.payment.dto;


public class PaymentReadyResponse {
    private String tid;
    private String next_redirect_pc_url;
    
    
	public PaymentReadyResponse(String tid, String redirectUrl) {
		this.tid = tid;
		this.next_redirect_pc_url = redirectUrl;
	}
	public String getTid() {
		return tid;
	}
	public void setTid(String tid) {
		this.tid = tid;
	}
	public String getNext_redirect_pc_url() {
		return next_redirect_pc_url;
	}
	public void setNext_redirect_pc_url(String next_redirect_pc_url) {
		this.next_redirect_pc_url = next_redirect_pc_url;
	}
}
