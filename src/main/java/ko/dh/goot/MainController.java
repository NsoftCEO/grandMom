package ko.dh.goot;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

	@GetMapping(value="/")
	public String main() {
		return "redirect:/view/html/main.html";
	}
}

