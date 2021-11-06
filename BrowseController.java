package com.erp.storage.controller;

import com.erp.aoplog.MyLog;
import com.erp.bean.City;
import com.erp.bean.Province;
import com.erp.bean.Users;
import com.erp.bean.Warehouse;
import com.erp.storage.biz.WarehouseBiz;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/storage/storageBrowse")
//将一起model 放入到HttpSession范围内
@SessionAttributes({"wPage", "w",  "plist", "clist"})
public class BrowseController {
    @Autowired
    private WarehouseBiz wbiz;

    //每次执行前会优先 将mid放入request范围
    @ModelAttribute
    public void initModuleData(HttpSession session) {
        session.removeAttribute("mid");
        session.setAttribute("mid", 11);
    }

    @RequestMapping("/list")
    @MyLog(value = "查看所有仓库列表")
    public String into(Model model, Warehouse w, @RequestParam(value = "pageNo",defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize",defaultValue = "5") Integer pageSize, HttpSession session){
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<Warehouse> wPage =null;
        if (loguser.getJobId()==1){
            wPage = wbiz.findAllByPage(null,null, null, null, pageNo, pageSize);
        }else {
            wPage = wbiz.findAllByPage(loguser.getUname(),null, null, null, pageNo, pageSize);
        }

        List<Warehouse> wlist = wPage.getList();
        //查询所有市名
        List<Province> prolist = wbiz.findAllProvince();
        //根据省名查询市名
        List<City> clist = wbiz.selectAllCityByProvinceName("");
        model.addAttribute("wPage",wPage);
        model.addAttribute("wlist",wlist);
        model.addAttribute("w",w);
        model.addAttribute("plist",prolist);
        model.addAttribute("clist",clist);
        return "storage/storageBrowse/storageBrowse";
    }

    @RequestMapping("/check")
    @MyLog(value = "查看仓库列表")
    public String check(Model model, HttpServletResponse response, Warehouse w, @RequestParam(value = "pageNo",defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize",defaultValue = "5") Integer pageSize,HttpSession session){
        response.addHeader("Cache-control","private");
        Users loguser = (Users)session.getAttribute("loguser");
        PageInfo<Warehouse> wPage =null;
        if (loguser.getJobId()==1){
            wPage = wbiz.findAllByPage(null,w.getcName(), w.getProvince().getpName(), w.getCity().getcName(), pageNo, pageSize);
        }else {
            wPage = wbiz.findAllByPage(loguser.getUname(),w.getcName(), w.getProvince().getpName(), w.getCity().getcName(), pageNo, pageSize);
        }
        List<Warehouse> wlist = wPage.getList();
        model.addAttribute("wPage",wPage);
        model.addAttribute("wlist",wlist);
        model.addAttribute("w",w);
        return "storage/storageBrowse/storageBrowse";
    }

    /**
     * 异步获取市
     * @param pname
     * @return
     */
    @RequestMapping("/storageBrowsegetcity")
    @ResponseBody
    public List<City> findcNameByPname(String pname){
        List<City> cities = wbiz.selectAllCityByProvinceName(pname);
        return cities;
    }

    /**
     * 详情
     * @param id
     * @param model
     * @return
     */
    @RequestMapping("/view/{id}")
    @MyLog(value = "查看仓库详情")
    public String findWarehouseById(@PathVariable("id") Integer id, Model model){
        Warehouse warehouse = wbiz.findWarehouseByID(id);
        model.addAttribute("w",warehouse);
        return "storage/storage/storageView";
    }
}
