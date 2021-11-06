package com.erp.storage.controller;

import com.erp.aoplog.MyLog;
import com.erp.bean.City;
import com.erp.bean.Province;
import com.erp.bean.Users;
import com.erp.bean.Warehouse;
import com.erp.storage.biz.WarehouseBiz;
import com.github.pagehelper.PageInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/storage/storage")
//将一起model 放入到HttpSession范围内
@SessionAttributes({"wPage", "plist", "clist"})
public class WarehouseController {
    @Autowired
    private WarehouseBiz wbiz;


    //每次执行前会优先 将mid放入request范围
    @ModelAttribute
    public void initModuleData(HttpSession session) {
        session.removeAttribute("mid");
        session.setAttribute("mid", 10);
    }

    @RequestMapping("/list")
    @MyLog(value = "查看仓库列表")
    public String into(Model model, @RequestParam(value = "pageNo",defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize",defaultValue = "5") Integer pageSize){
        Warehouse w=new Warehouse();
        PageInfo<Warehouse> wPage = wbiz.findAllByPage(null,null, null, null, pageNo, pageSize);
        //查询所有市名
        List<Province> prolist = wbiz.findAllProvince();
        //根据省名查询市名
        List<City> clist = wbiz.selectAllCityByProvinceName("");
        List<Warehouse> wlist = wPage.getList();
        model.addAttribute("wPage",wPage);
        model.addAttribute("wlist",wlist);
        model.addAttribute("w",w);
        model.addAttribute("plist",prolist);
        model.addAttribute("clist",clist);
        return "storage/storage/storageList";
    }

    @RequestMapping("/check")
    @MyLog(value = "条件查询仓库")
    public String check(Model model, HttpServletResponse response, Warehouse w, @RequestParam(value = "pageNo",defaultValue = "1") Integer pageNo, @RequestParam(value = "pageSize",defaultValue = "5") Integer pageSize){
        response.addHeader("Cache-control","private");
        PageInfo<Warehouse> wPage = wbiz.findAllByPage(null,w.getcName(), w.getProvince().getpName(), w.getCity().getcName(), pageNo, pageSize);
        List<Warehouse> wlist = wPage.getList();
        model.addAttribute("wPage",wPage);
        model.addAttribute("wlist",wlist);
        model.addAttribute("w",w);
        return "storage/storage/storageList";
    }

    /**
     * 异步根据省获取城市
     * @param pname
     * @return
     */
    @RequestMapping("/storagelistgetcity")
    @ResponseBody
    public List<City> findcNameByPname(String pname){
        List<City> cities = wbiz.selectAllCityByProvinceName(pname);
        return cities;
    }

    /**
     * 查看详情
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

    /**
     * 注销恢复
     * @return
     */
    @RequestMapping("/zxhf")
    @MyLog(value = "仓库注销、恢复")
    public String restorecancellation(@RequestParam("id") Integer id, @RequestParam("state") String state, @RequestParam(value = "pageNo1",defaultValue = "1") Integer pageNo1){
        System.out.println(id+"  "+state);
        Boolean flas = wbiz.updateStateById(id, state);
        return "forward:/storage/storage/list?pageNo="+pageNo1;
    }

    /**
     * 跳转修改页面
     * @return
     */
    @RequestMapping("/goupdate/{id}")
    public String goupdateStorage(@PathVariable("id") Integer id, Model model){
        //查询所有市名
        List<Province> prolist = wbiz.findAllProvince();
        //查询仓库管理员
        List<Users> userlist = wbiz.findUsersByJobId(8);

        Warehouse warehouse = wbiz.findWarehouseByID(id);
        model.addAttribute("warehouse",warehouse);
        model.addAttribute("plist",prolist);
        model.addAttribute("userlist",userlist);
        return "storage/storage/storageUpdate";
    }
    @RequestMapping("/update")
    @MyLog(value = "修改仓库信息")
    public String updateStorage(Warehouse warehouse){
        Boolean flas = wbiz.updateByPrimaryKeySelective(warehouse);
        System.out.println(flas);
        return "forward:/storage/storage/list";
    }

    /**
     * 异步根据省获取城市
     * @param pname
     * @return
     */
    @RequestMapping("/goupdate/findcNameByPnameInupdate")
    @ResponseBody
    public List<City> findcNameByPnameInupdate(String pname){
        List<City> cities = wbiz.selectAllCityByProvinceName(pname);
        return cities;
    }

    /**
     * 跳转添加页面
     * @param model
     * @return
     */
    @RequestMapping("/goadd")
    public String goadd(Model model){
        //查询所有市名
        List<Province> prolist = wbiz.findAllProvince();
        //查询仓库管理员
        List<Users> userlist = wbiz.findUsersByJobId(8);

        model.addAttribute("plist",prolist);
        model.addAttribute("userlist",userlist);
        return "storage/storage/storageAdd";
    }

    /**
     * 添加仓库信息
     * @param w
     * @return
     */
    @RequestMapping("/add")
    @MyLog(value = "添加仓库信息")
    public String add(Warehouse w){
        w.setCreationTime(new Date());
        wbiz.insertSelective(w);
        return "forward:/storage/storage/list";
    }
}
