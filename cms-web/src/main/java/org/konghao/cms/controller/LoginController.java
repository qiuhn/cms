package org.konghao.cms.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.konghao.basic.util.Captcha;
import org.konghao.cms.model.Role;
import org.konghao.cms.model.RoleType;
import org.konghao.cms.model.User;
import org.konghao.cms.service.IUserService;
import org.konghao.cms.web.CmsSessionContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class LoginController {
	private IUserService userService;

	public IUserService getUserService() {
		return userService;
	}
	
	@Inject
	public void setUserService(IUserService userService) {
		this.userService = userService;
	}
	
	@RequestMapping(value="/login",method=RequestMethod.GET)
	public String login() {
		System.out.println("-----------------LoginController.login()");
		return "admin/login";
	}
	
	@RequestMapping(value="/login",method=RequestMethod.POST)
	public String login(String username,String password,String checkcode,Model model,HttpSession session) {
		String cc = (String)session.getAttribute("cc");
		if(!cc.equals(checkcode)) {
			model.addAttribute("error","验证码出错，请重新输入");
			return "admin/login";
		}
		User loginUser = userService.login(username, password);
		session.setAttribute("loginUser", loginUser);
		List<Role> rs = userService.listUserRoles(loginUser.getId());
		boolean isAdmin = isRole(rs,RoleType.ROLE_ADMIN);
		session.setAttribute("isAdmin", isAdmin);
		if(!isAdmin) {
			session.setAttribute("allActions", getAllActions(rs, session));
			session.setAttribute("isAudit", isRole(rs,RoleType.ROLE_AUDIT));
			session.setAttribute("isPublish", isRole(rs,RoleType.ROLE_PUBLISH));
		}
		session.removeAttribute("cc");
		CmsSessionContext.addSessoin(session);
		return "redirect:/admin";

	}

	@SuppressWarnings("unchecked")
	private Set<String> getAllActions(List<Role> rs,HttpSession session) {
		Set<String> actions = new HashSet<String>();
		Map<String,Set<String>> allAuths = (Map<String,Set<String>>)session.getServletContext().getAttribute("allAuths");
		actions.addAll(allAuths.get("base"));
		for(Role r:rs) {
			if(r.getRoleType()==RoleType.ROLE_ADMIN) continue;
			actions.addAll(allAuths.get(r.getRoleType().name()));
		}
		return actions;
	}
	
	
	private boolean isRole(List<Role> rs,RoleType rt) {
		for(Role r:rs) {
			if(r.getRoleType()==rt) return true;
		}
		return false;
	}
	
	
	@RequestMapping("/drawCheckCode")
	public void drawCheckCode(HttpServletResponse resp,HttpSession session) throws IOException {
		resp.setContentType("image/jpg");
		int width = 200;
		int height = 30;
		Captcha c = Captcha.getInstance();
		c.set(width, height);
		String checkcode = c.generateCheckcode();
		session.setAttribute("cc", checkcode);
		OutputStream os = resp.getOutputStream();
		ImageIO.write(c.generateCheckImg(checkcode), "jpg", os);
	}
}
