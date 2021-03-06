/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
*/
package com.agnux.kemikal.controllers;

import com.agnux.cfd.v2.Base64Coder;
import com.agnux.common.helpers.FileHelper;
import com.agnux.common.helpers.StringHelper;
import com.agnux.common.helpers.TimeHelper;
import com.agnux.common.obj.DataPost;
import com.agnux.common.obj.ResourceProject;
import com.agnux.common.obj.UserSessionData;
import com.agnux.kemikal.interfacedaos.GralInterfaceDao;
import com.agnux.kemikal.interfacedaos.HomeInterfaceDao;
import com.agnux.kemikal.interfacedaos.ProInterfaceDao;
import com.agnux.kemikal.reportes.PdfProOrdenProduccion;
import com.agnux.kemikal.reportes.PdfProOrdenRequisicion;
import com.agnux.kemikal.reportes.PdfProSimulacionProduccion;
import com.itextpdf.text.DocumentException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 *
 * @author agnux
 * En esta clase se utiliza una macro "macrocoma", para sistituir las comas en la lista de procedidmientos 
 * 
 */

@Controller
@SessionAttributes({"user"})
@RequestMapping("/proordensimulacion/")
public class ProOrdenSimulacionController {
    private static final Logger log  = Logger.getLogger(ProOrdenSimulacionController.class.getName());
    ResourceProject resource = new ResourceProject();
    
    
    @Autowired
    @Qualifier("daoPro")
    private ProInterfaceDao daoPro;
    
    
    public ProInterfaceDao getProDao() {
        return daoPro;
    }
    
    @Autowired
    @Qualifier("daoHome")
    private HomeInterfaceDao HomeDao;
    
    public HomeInterfaceDao getHomeDao() {
        return HomeDao;
    }
    
    @Autowired
    @Qualifier("daoGral")
    private GralInterfaceDao gralDao;
    
    public GralInterfaceDao getGralDao() {
        return gralDao;
    }
    
    @RequestMapping(value="/startup.agnux")
    public ModelAndView startUp(HttpServletRequest request, HttpServletResponse response, 
            @ModelAttribute("user") UserSessionData user)
            throws ServletException, IOException {
        
        log.log(Level.INFO, "Ejecutando starUp de {0}", ProOrdenSimulacionController.class.getName());
        LinkedHashMap<String,String> infoConstruccionTabla = new LinkedHashMap<String,String>();
        
        //infoConstruccionTabla.put("id", "Acciones:90");
        
        ModelAndView x = new ModelAndView("proordensimulacion/startup", "title", "Simulaci&oacute;n de Producci&oacute;n");
        
        x = x.addObject("layoutheader", resource.getLayoutheader());
        x = x.addObject("layoutmenu", resource.getLayoutmenu());
        x = x.addObject("layoutfooter", resource.getLayoutfooter());
        x = x.addObject("grid", resource.generaGrid(infoConstruccionTabla));
        x = x.addObject("url", resource.getUrl(request));
        x = x.addObject("username", user.getUserName());
        x = x.addObject("empresa", user.getRazonSocialEmpresa());
        x = x.addObject("empresa", user.getRazonSocialEmpresa());
        
        String userId = String.valueOf(user.getUserId());
        
        String codificado = Base64Coder.encodeString(userId);
        
        //id de usuario codificado
        x = x.addObject("iu", codificado);
        
        return x;
    }
    
    
    
    
    @RequestMapping(value="/get_all_ordensimulacion.json", method = RequestMethod.POST)
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, Object>>> getAllPreordenesProduccionJson(
           @RequestParam(value="orderby", required=true) String orderby,
           @RequestParam(value="desc", required=true) String desc,
           @RequestParam(value="items_por_pag", required=true) int items_por_pag,
           @RequestParam(value="pag_start", required=true) int pag_start,
           @RequestParam(value="display_pag", required=true) String display_pag,
           @RequestParam(value="input_json", required=true) String input_json,
           @RequestParam(value="cadena_busqueda", required=true) String cadena_busqueda,
           @RequestParam(value="iu", required=true) String id_user_cod,
           Model modcel) {
        
        HashMap<String,ArrayList<HashMap<String, Object>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, Object>>>();
        HashMap<String,String> has_busqueda = StringHelper.convert2hash(StringHelper.ascii2string(cadena_busqueda));
        
        //aplicativo Simulacion de produccion
        Integer app_selected = 116;
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        
        //variables para el buscador
        String folio_orden = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("folio_orden")))+"%";
        String tipo_orden = has_busqueda.get("tipo_orden").equals(null) ? "0" : has_busqueda.get("tipo_orden");
        String sku_producto_busqueda = "%"+StringHelper.isNullString(String.valueOf(has_busqueda.get("sku_producto_busqueda")))+"%";
        
        String data_string = app_selected+"___"+id_usuario+"___"+folio_orden+"___"+tipo_orden+"___"+sku_producto_busqueda;
        
        //obtiene total de registros en base de datos, con los parametros de busqueda
        int total_items = this.getProDao().countAll(data_string);
        
        //calcula el total de paginas
        int total_pags = resource.calculaTotalPag(total_items,items_por_pag);
        
        //variables que necesita el datagrid, para no tener que hacer uno por cada aplicativo
        DataPost dataforpos = new DataPost(orderby, desc, items_por_pag, pag_start, display_pag, input_json, cadena_busqueda,total_items,total_pags,id_user_cod);
        
        int offset = resource.__get_inicio_offset(items_por_pag, pag_start);
        
        //obtiene los registros para el grid, de acuerdo a los parametros de busqueda
        jsonretorno.put("Data", this.getProDao().getProOrden_PaginaGrid(data_string, offset, items_por_pag, orderby, desc));
        
        //obtiene el hash para los datos que necesita el datagrid
        jsonretorno.put("DataForGrid", dataforpos.formaHashForPos(dataforpos));
        
        return jsonretorno;
    }
    
    
    
    //obtiene datos para el autopletar
    @RequestMapping(value="/get_proordentipos.json", method = RequestMethod.POST)
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getProOrdenTiposJson(
           @RequestParam(value="iu", required=true) String id_user_cod,
           Model modcel) {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> tc = new ArrayList<HashMap<String, String>>();
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        jsonretorno.put("ordenTipos", this.getProDao().getProOrdenTipos(id_empresa));
        
        return jsonretorno;
    }
    
    
    
    //Obtiene versiones de la formula
    @RequestMapping(method = RequestMethod.POST, value="/get_versiones_formulas_por_sku.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> get_versiones_formulas_por_skuJson(
            @RequestParam(value="sku", required=true) String sku,
            @RequestParam(value="tipo", required=true) String tipo,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando get_versiones_formulas_por_sku de {0}", ProOrdenProduccionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> formulas = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        formulas = this.getProDao().getVersionesFormulasPorCodigoProducto(sku, tipo, id_empresa);
        
        jsonretorno.put("formulas", formulas);
        
        return jsonretorno;
    }
    
    
    
    //Obtiene datos para la simulacion
    @RequestMapping(method = RequestMethod.POST, value="/get_productos_formula_prod.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getProductosFormulaJson(
            @RequestParam(value="id_formula", required=true) String id_formula,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
        ) {
        
        log.log(Level.INFO, "Ejecutando getProductosJson de {0}", ProOrdenProduccionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> equivalentes = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        equivalentes = this.getProDao().getProductosFormula(id_formula);
        
        jsonretorno.put("productos", equivalentes);
        
        return jsonretorno;
    }
    
    
    
    
    
    
    
    //http://localhost:8080//com.mycompany_Kemikal_war_1.0-SNAPSHOT/controllers/proordensimulacion/getPdfSimulaProduccion/2/576/100/MQ==/out.json
    //Genera pdf de formulacion de
    @RequestMapping(value = "/getPdfSimulaProduccion/{tipo}/{id_formula}/{cantidad_kg}/{cantidad_lt}/{densidad}/{iu}/out.json", method = RequestMethod.GET ) 
    public ModelAndView getPdfSimulaProduccionJson(
                @PathVariable("tipo") String tipo,
                @PathVariable("id_formula") String id_formula,
                @PathVariable("cantidad_kg") String cantidad_kg,
                @PathVariable("cantidad_lt") String cantidad_lt,
                @PathVariable("densidad") String densidad,
                @PathVariable("iu") String id_user,
                HttpServletRequest request, 
                HttpServletResponse response, 
                Model model)
            throws ServletException, IOException, URISyntaxException, DocumentException, Exception {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> empleado = new HashMap<String, String>();
        HashMap<String, String> datosEncabezadoPie= new HashMap<String, String>();
        ArrayList<HashMap<String, String>> datos = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> componentes = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> tipos = new ArrayList<HashMap<String, String>>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        Integer app_selected = 116;//Simulacion de produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_empleado = Integer.parseInt(userDat.get("empleado_id"));
        
        empleado = this.getProDao().getNombreEmpleadoById(id_empleado);
        String razon_social_empresa = this.getGralDao().getRazonSocialEmpresaEmisora(id_empresa);
        
        SimpleDateFormat formato = new SimpleDateFormat("'Generado el' d 'de' MMMMM 'del' yyyy 'a las' HH:mm:ss 'hrs.'");
        String periodo = formato.format(new Date());
        
        if(!empleado.isEmpty()){
            datosEncabezadoPie.put("usuario_elaboracion", empleado.get("nombre_usuario"));
            System.out.println("usuario_elaboracion: "+empleado.get("nombre_usuario"));
        }else{
            datosEncabezadoPie.put("usuario_elaboracion", "");
            System.out.println("usuario_elaboracion: vacio");
        }
        
        datosEncabezadoPie.put("nombre_empresa_emisora", razon_social_empresa);
        //datosEncabezadoPie.put("titulo_reporte", this.getGralDao().getTituloReporte(id_empresa, app_selected));
        datosEncabezadoPie.put("titulo_reporte", "Simulación de Producción");
        datosEncabezadoPie.put("periodo", periodo);
        datosEncabezadoPie.put("codigo1", this.getGralDao().getCodigo1Iso(id_empresa, app_selected));
        datosEncabezadoPie.put("codigo2", this.getGralDao().getCodigo2Iso(id_empresa, app_selected));
        
        //obtener el directorio temporal
        String dir_tmp = this.getGralDao().getTmpDir();
        
        File file_dir_tmp = new File(dir_tmp);
        System.out.println("Directorio temporal: "+file_dir_tmp.getCanonicalPath());
        
        //Obtiene los datos de la Formula para el PDF de la Simulacion
        datos = this.getProDao().getProductoFormulaPdfSimulacion(Integer.parseInt(id_formula));
        tipos = this.getProDao().getProOrdenTipoById(Integer.parseInt(tipo));
        
        if(tipos.size()>0){
            datosEncabezadoPie.put("tipo", tipos.get(0).get("titulo"));
        }else{
            datosEncabezadoPie.put("tipo", "");
        }
        
        if(datos.size()>0){
            datosEncabezadoPie.put("codigo", datos.get(0).get("sku"));
            datosEncabezadoPie.put("descripcion", datos.get(0).get("descripcion"));
            datosEncabezadoPie.put("version", datos.get(0).get("version"));
        }else{
            datosEncabezadoPie.put("codigo", "");
            datosEncabezadoPie.put("descripcion", "");
            datosEncabezadoPie.put("version", "");
        }
        
        if(cantidad_kg.equals("")){
            datosEncabezadoPie.put("cantidad", "0");
        }else{
            datosEncabezadoPie.put("cantidad", cantidad_kg);
        }
        
        if(cantidad_lt.equals("")){
            datosEncabezadoPie.put("cantidad_lt", "0");
        }else{
            datosEncabezadoPie.put("cantidad_lt", cantidad_lt);
        }
        
        if(densidad.equals("")){
            datosEncabezadoPie.put("densidad", "0");
        }else{
            datosEncabezadoPie.put("densidad", densidad);
        }
        
        
        
        //Obtiene los componentes de la Formula
        componentes = this.getProDao().getProductosFormula(id_formula);
        
        String file_name = "simulacion_"+datosEncabezadoPie.get("codigo") +".pdf";
        
        //ruta de archivo de salida
        String fileout = file_dir_tmp +"/"+  file_name;
        
        datosEncabezadoPie.put("fileout", fileout);
        
        
        //instancia a la clase que construye el pdf del reporte de facturas
        PdfProSimulacionProduccion pdf = new PdfProSimulacionProduccion(datosEncabezadoPie, componentes);
        pdf.ViewPDF();
        
        
        System.out.println("Recuperando archivo: " + fileout);
        File file = new File(fileout);
        if (file.exists()){
            int size = (int) file.length(); // Tama√±o del archivo
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            response.setBufferSize(size);
            response.setContentLength(size);
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition","attachment; filename=\"" + file.getCanonicalPath() +"\"");
            FileCopyUtils.copy(bis, response.getOutputStream());
            response.flushBuffer();
            
            //Eliminar el archivo despues de visualizar en el navegador
            FileHelper.delete(fileout);
        }
        
        return null;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    /*
    @RequestMapping(method = RequestMethod.POST, value="/get_datos_orden.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getProOrdenJson(
            @RequestParam(value="id_orden", required=true) Integer id,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando get_datos_orden de {0}", ProOrdenSimulacionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        
        
        ArrayList<HashMap<String, String>> datosOrden = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> datosOrdenDet = new ArrayList<HashMap<String, String>>();
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        Integer id_sucursal = Integer.parseInt(userDat.get("sucursal_id"));
        
        if( id != 0 ){
            datosOrden = this.getProDao().getProOrden_Datos(id);
            
            if(datosOrden.get(0).get("pro_proceso_flujo_id").equals("3")){
                
                datosOrdenDet = this.getProDao().getProOrden_EspecificacionesDetalle(id);
                
                
            }else{
                
                datosOrdenDet = this.getProDao().getProOrden_Detalle(id);
                
            }
            
            
        }
        
        jsonretorno.put("Orden", datosOrden);
        jsonretorno.put("OrdenDet", datosOrdenDet);
        jsonretorno.put("ordenTipos", this.getProDao().getProOrdenTipos(id_empresa));
        jsonretorno.put("Instrumentos", this.getProDao().getInstrumentos(id_empresa));
        return jsonretorno;
    }
    
    
    
    
    //obtiene los productos para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/get_buscador_pedidos.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getBuscadorPedidosJson(
            @RequestParam(value="folio", required=true) String folio,
            @RequestParam(value="proveedor", required=true) String proveedor,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getProductosJson de {0}", ProOrdenSimulacionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> pedidos = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        folio = "%"+StringHelper.isNullString(String.valueOf(folio))+"%";
        
        proveedor = "%"+StringHelper.isNullString(String.valueOf(proveedor))+"%";
        
        pedidos = this.getProDao().getBuscadorPedidos(folio, proveedor,id_empresa);
        
        jsonretorno.put("pedidos", pedidos);
        
        return jsonretorno;
    }
    
    
    //crear y editar una  formula
    @RequestMapping(method = RequestMethod.POST, value="/guarda_especificaciones.json")
    public @ResponseBody HashMap<String, String> guardaEspecificacionesJson(
            @RequestParam(value="id", required=true) Integer id,
        @RequestParam(value="tipoorden", required=true) String tipoorden,
        @RequestParam(value="cadena", required=true) String cadena,
        @RequestParam(value="iu", required=true) String user,
        Model model
            ) {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> succes = new HashMap<String, String>();
        Integer app_selected = 93;//catalogo de preorden produccion
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //String extra_data_array = "'sin datos'";
        String actualizo = "0";
        int no_partida = 0;
        
        String command_selected1 = "";
        
        String arreglo[];
            //serializar el arreglo
            String extra_data_array = "";
            
            if(!cadena.equals("") && cadena.length() > 5){
                cadena += "'"+cadena+"'";
                arreglo = cadena.split("\\$\\$\\$\\$");
                extra_data_array = StringUtils.join(arreglo, "','");
            }else{
                extra_data_array = "'Sin Datos'";
            }
            
        if( id ==0 ){
            command_selected1 = "new";
        }else{
            command_selected1 = "edit";
        }
        
        String data_string = app_selected+"___"+command_selected1+"___"+id_usuario+"___"+id+"___"+tipoorden;
        
        succes = this.getProDao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
        
        log.log(Level.INFO, "despues de validacion {0}", String.valueOf(succes.get("success")));
        if( String.valueOf(succes.get("success")).equals("true") ){
            actualizo = this.getProDao().selectFunctionForApp_Produccion(data_string, extra_data_array);
        }
        
        jsonretorno.put("success",String.valueOf(succes.get("success")));
        
        log.log(Level.INFO, "Salida json {0}", String.valueOf(jsonretorno.get("success")));
        return jsonretorno;
    }
    
    //obtiene datos del Lote
    @RequestMapping(method = RequestMethod.POST, value="/getDatosLote.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getDataLineasJson(
            @RequestParam(value="no_lote", required=true) String no_lote,
            @RequestParam(value="id_producto", required=true) Integer id_producto,
            @RequestParam(value="iu", required=true) String id_user_cod,
            Model model
        ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> arrayTiposProducto = new ArrayList<HashMap<String, String>>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        
        arrayTiposProducto=this.getProDao().getProOrdenProd_DatosLote(no_lote, id_producto, id_usuario);
        
        jsonretorno.put("Lote", arrayTiposProducto);
        return jsonretorno;
    }
    
    

    //genera la requisicion
    //send_requisicion_op
    //crear y editar una  formula
    @RequestMapping(method = RequestMethod.POST, value="/send_requisicion_op.json")
    public @ResponseBody HashMap<String, String> guardaRequisicionJson(
            @RequestParam(value="id", required=true) Integer id,
            @RequestParam(value="data_string", required=true) String cadena,
            @RequestParam(value="command_selected", required=true) String command_selected,
            @RequestParam(value="iu", required=true) String user,
                Model model
            ) {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> succes = new HashMap<String, String>();
        Integer app_selected = 93;//catalogo de preorden produccion
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //String extra_data_array = "'sin datos'";
        String actualizo = "0";
        int no_partida = 0;
        String accion = "";
        String command_selected1 = "";
        
        String arreglo[];
        //serializar el arreglo
        String extra_data_array = "";
        accion = "requisicion";
        
        if(!cadena.equals("") && cadena.length() > 2){
            cadena = "'"+cadena+"'";
            arreglo = cadena.split("\\$\\$\\$\\$");
            extra_data_array = StringUtils.join(arreglo, "','");
        }else{
            extra_data_array = "'Sin Datos'";
        }
        
        if( id ==0 ){
            command_selected1 = "new";
        }else{
            command_selected1 = "edit";
        }
        
        
        String data_string = app_selected+"___"+command_selected1+"___"+id_usuario+"___"+id+"___0___0___0___"+command_selected+"___"+accion;
        
        succes = this.getProDao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
        
        log.log(Level.INFO, "despues de validacion {0}", String.valueOf(succes.get("success")));
        if( String.valueOf(succes.get("success")).equals("true") ){
            actualizo = this.getProDao().selectFunctionForApp_Produccion(data_string, extra_data_array);
        }
        
        jsonretorno.put("success",String.valueOf(succes.get("success")));
        
        log.log(Level.INFO, "Salida json {0}", String.valueOf(jsonretorno.get("success")));
        
        return jsonretorno;
        
        
    }
    

    
    //crear y editar una  formula
    @RequestMapping(method = RequestMethod.POST, value="/guarda_lotes.json")
    public @ResponseBody HashMap<String, String> guardaLotesJson(
            @RequestParam(value="id", required=true) Integer id,
        @RequestParam(value="id_prod", required=true) Integer id_prod,
        @RequestParam(value="tipoorden", required=true) String tipoorden,
        @RequestParam(value="id_subproceso", required=true) Integer id_subproceso,
        @RequestParam(value="observaciones", required=true) String observaciones,
        @RequestParam(value="command_selected", required=true) String command_selected,
        @RequestParam(value="fecha_elavorar", required=true) String fecha_elavorar,
        @RequestParam(value="cadena", required=true) String cadena,
        @RequestParam(value="iu", required=true) String user,
        Model model
            ) {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> succes = new HashMap<String, String>();
        Integer app_selected = 93;//catalogo de preorden produccion
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //String extra_data_array = "'sin datos'";
        String actualizo = "0";
        int no_partida = 0;
        String accion = "";
        String command_selected1 = "";
        
        String arreglo[];
        //serializar el arreglo
        String extra_data_array = "";
        accion = "lotes";
        if(!cadena.equals("") && cadena.length() > 2){
            cadena = "'"+cadena+"'";
            arreglo = cadena.split("\\$\\$\\$\\$");
            extra_data_array = StringUtils.join(arreglo, "','");
        }else{
            extra_data_array = "'Sin Datos'";
        }
        
        if( id ==0 ){
            command_selected1 = "new";
        }else{
            command_selected1 = "edit";
        }
        
        
        String data_string = app_selected+"___"+command_selected1+"___"+id_usuario+"___"+id+"___"+tipoorden+"___"+fecha_elavorar+"___"+observaciones+"___"+command_selected+"___"+accion;
        
        succes = this.getProDao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
        
        log.log(Level.INFO, "despues de validacion {0}", String.valueOf(succes.get("success")));
        if( String.valueOf(succes.get("success")).equals("true") ){
            actualizo = this.getProDao().selectFunctionForApp_Produccion(data_string, extra_data_array);
        }
        
        jsonretorno.put("success",String.valueOf(succes.get("success")));
        
        log.log(Level.INFO, "Salida json {0}", String.valueOf(jsonretorno.get("success")));
        
        return jsonretorno;
        
    }
    

    
    //obtiene los productos para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/get_productos_pedido.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getProductosPedidoJson(
            @RequestParam(value="id_pedido", required=true) String id_pedido,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getProductosJson de {0}", ProOrdenSimulacionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> productos = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        productos = this.getProDao().getProductosPedidoSeleccionado(id_pedido, id_usuario);
        
        jsonretorno.put("productos", productos);
        
        return jsonretorno;
    }
    

    //crear y editar una  formula
    @RequestMapping(method = RequestMethod.POST, value="/edit.json")
    public @ResponseBody HashMap<String, String> editFormulasJson(
        @RequestParam(value="id_orden", required=true) Integer id,
        @RequestParam(value="tipoorden", required=true) String tipoorden,
        @RequestParam(value="fecha_elavorar", required=true) String fecha_elavorar,
        @RequestParam(value="observaciones", required=true) String observaciones,
        @RequestParam(value="command_selected", required=true) String command_selected,
        @RequestParam(value="especificaicones_lista", required=true) String especificaicones_lista,
        
        @RequestParam(value="eliminar", required=true) String[] eliminar,
        @RequestParam(value="id_reg", required=true) String[] id_reg,
        @RequestParam(value="inv_prod_id", required=true) String[] inv_prod_id,
        @RequestParam(value="subproceso_id", required=false) String[] subproceso_id,
        @RequestParam(value="pro_subproceso_prod_id", required=false) String[] pro_subproceso_prod_id,
        @RequestParam(value="persona", required=false) String[] persona,
        @RequestParam(value="equipo", required=false) String[] equipo,
        @RequestParam(value="eq_adicional", required=false) String[] eq_adicional,
        @RequestParam(value="cantidad", required=false) String[] cantidad,
        @RequestParam(value="unidad_default", required=false) String[] unidad_default,
        @RequestParam(value="unidad_id", required=false) String[] unidad_id,
        @RequestParam(value="densidad", required=false) String[] densidad,
        Model model,@ModelAttribute("user") UserSessionData user
    ) {
        
        HashMap<String, String> jsonretorno = new HashMap<String, String>();
        HashMap<String, String> succes = new HashMap<String, String>();
        Integer app_selected = 93;//catalogo de preorden produccion
        
        Integer id_usuario= user.getUserId();//variable para el id  del usuario
        //String extra_data_array = "'sin datos'";
        String actualizo = "0";
        int no_partida = 0;
        
        String command_selected1 = "";
        
        String extra_data_array = "";
        String arreglo[];
        arreglo = new String[id_reg.length];
        String accion = "";
        if(command_selected.equals("3")){
            accion = "especificaiocnes";
            if(!especificaicones_lista.equals("") && especificaicones_lista.length() > 2){
                
                especificaicones_lista = "'"+especificaicones_lista+"'";
                arreglo = especificaicones_lista.split("\\$\\$\\$\\$");
                extra_data_array = StringUtils.join(arreglo, "','");
            }else{
                extra_data_array = "'Sin Datos'";
            }
        }else{
            if( command_selected.equals("2") || command_selected.equals("1")){
                for(int i=0; i<id_reg.length; i++) {
                    if(Integer.parseInt(eliminar[i]) != 0){
                        no_partida++;//si no esta eliminado incrementa el contador de partidas
                    }
                    
                    arreglo[i]= "'"+eliminar[i] +"___" + id_reg[i]+"___" + inv_prod_id[i]+"___" + subproceso_id[i]+"___"+ pro_subproceso_prod_id[i]+"___"+ 
                            persona[i]+"___"+ equipo[i]+"___"+ eq_adicional[i]+"___"+ cantidad[i]+"___" + no_partida+"___"+ unidad_default[i]+"___"+ densidad[i]+"___"+ unidad_id[i]+"'";
                    
                    //System.out.println(arreglo[i]);
                }
                
                //serializar el arreglo
                extra_data_array = StringUtils.join(arreglo, ",");
            }else{
                if(command_selected.equals("4") ){
                    
                    accion = "";
                    
                    for(int i=0; i<id_reg.length; i++) {
                        if(Integer.parseInt(eliminar[i]) != 0){
                            no_partida++;//si no esta eliminado incrementa el contador de partidas
                        }
                        
                        //                  1                   2                   3                       4                       5                               
                        arreglo[i]= "'"+eliminar[i] +"___" + id_reg[i]+"___" + inv_prod_id[i]+"___" + 
                                //   4                   5                      6                  7                  8
                                cantidad[i]+"___" + no_partida+"___"+ unidad_default[i]+"___"+ densidad[i]+"___"+ unidad_id[i]+"'";
                        
                        //System.out.println(arreglo[i]);
                    }
                    
                    extra_data_array = StringUtils.join(arreglo, ",");
                    
                    //esto estaba antes de que se actualizara la cantidad al finalizar ala orden de produccion

                }else{
                    extra_data_array = "'Sin Datos'";
                }
            }
        }
        
        
        if( id ==0 ){
            command_selected1 = "new";
        }else{
            command_selected1 = "edit";
        }
        
        
        
        String data_string = app_selected+"___"+command_selected1+"___"+id_usuario+"___"+id+"___"+tipoorden+"___"+fecha_elavorar+"___"+observaciones+"___"+command_selected+"___"+accion;
        
        succes = this.getProDao().selectFunctionValidateAaplicativo(data_string,app_selected,extra_data_array);
        
        log.log(Level.INFO, "despues de validacion {0}", String.valueOf(succes.get("success")));
        if( String.valueOf(succes.get("success")).equals("true") ){
            actualizo = this.getProDao().selectFunctionForApp_Produccion(data_string, extra_data_array);
        }
        
        jsonretorno.put("success",String.valueOf(succes.get("success")));
        
        log.log(Level.INFO, "Salida json {0}", String.valueOf(jsonretorno.get("success")));
        return jsonretorno;
    }
    
     * 
     */
    
    
    //obtiene lineas de producto y datos para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/getProductoTipos.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getDataLineasJson(
            @RequestParam(value="iu", required=true) String id_user_cod,
            Model model
            ) {
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        //ArrayList<HashMap<String, String>> arrayLineas = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> arrayTiposProducto = new ArrayList<HashMap<String, String>>();
        //HashMap<String, String> cadenaLineas = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        
        arrayTiposProducto=this.getProDao().getProducto_Tipos();
        
        //cadenaLineas.put("cad_lineas", genera_treeview( this.getInvDao().getProducto_Lineas() ));
        //arrayLineas.add(cadenaLineas);
        //jsonretorno.put("Lines",arrayLineas);
        jsonretorno.put("prodTipos", arrayTiposProducto);
        
        return jsonretorno;
    }
    
    
    
    
    //obtiene los productos para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/get_buscador_productos.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getProductosJson(
            @RequestParam(value="sku", required=true) String sku,
            @RequestParam(value="tipo", required=true) String tipo,
            @RequestParam(value="descripcion", required=true) String descripcion,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getProductosJson de {0}", ProOrdenSimulacionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> productos = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        productos = this.getProDao().getBuscadorProductos(sku, tipo, descripcion, id_empresa);
        
        jsonretorno.put("productos", productos);
        
        return jsonretorno;
    }
    
    
    
    /*
    //obtiene los productos equivalentes para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/get_buscador_equivalentes.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getEquivalentesJson(
            @RequestParam(value="id_producto", required=true) String id_producto,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getProductosJson de {0}", ProOrdenSimulacionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> equivalentes = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        equivalentes = this.getProDao().getBuscadorEquivalentes(id_producto, id_empresa, id_usuario);
        
        jsonretorno.put("equivalentes", equivalentes);
        
        return jsonretorno;
    }

    
    //obtiene los productos para el buscador
    @RequestMapping(method = RequestMethod.POST, value="/get_requisicion_orden_prod.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getgetRequisicionOPJson(
            @RequestParam(value="id_orden", required=true) String id_orden,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        
        log.log(Level.INFO, "Ejecutando getProductosJson de {0}", ProOrdenSimulacionController.class.getName());
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        ArrayList<HashMap<String, String>> equivalentes = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        userDat = this.getHomeDao().getUserById(id_usuario);
        
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        equivalentes = this.getProDao().getRequisicionOP(id_orden, id_empresa, id_usuario);
        
        jsonretorno.put("requisicion", equivalentes);
        
        return jsonretorno;
    }
     * 
     */
    
    //Busca un sku en especifico
    @RequestMapping(method = RequestMethod.POST, value="/get_busca_sku_prod.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> getBuscaSkuProdJson(
            @RequestParam(value="sku", required=true) String sku,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        jsonretorno.put("Sku", this.daoPro.getProProductoPorSku(sku.toUpperCase(), id_empresa));
        jsonretorno.put("SubProcesos", this.daoPro.getProSubprocesosPorProductoSku(sku.toUpperCase(), id_empresa, "0", "0"));
        
        return jsonretorno;
    }
    
    
    /*
    //Busca un sku en especifico
    @RequestMapping(method = RequestMethod.POST, value="/detalle_elementos_prod_formula.json")
    public @ResponseBody HashMap<String,ArrayList<HashMap<String, String>>> detalleElementosProdFormulaJson(
            @RequestParam(value="id_orden", required=true) String id_orden,
            @RequestParam(value="id_subproceso", required=true) String id_subproceso,
            @RequestParam(value="id_producto", required=true) String id_producto,
            @RequestParam(value="iu", required=true) String id_user,
            Model model
            ) {
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        HashMap<String,ArrayList<HashMap<String, String>>> jsonretorno = new HashMap<String,ArrayList<HashMap<String, String>>>();
        jsonretorno.put("productos", this.daoPro.getProElementosProducto(id_producto, id_orden, id_subproceso));
        
        return jsonretorno;
    }
     */
    
    //obtiene datos para el autopletar
    @RequestMapping(value="/get_autocomplete_operarios.json", method = RequestMethod.POST)
    public @ResponseBody ArrayList<HashMap<String, String>> getProOrdenOperariosJson(
           @RequestParam(value="cadena", required=true) String cadena,
           @RequestParam(value="iu", required=true) String id_user_cod,
           Model modcel) {
        
        ArrayList<HashMap<String, String>> arrayOperarios = new ArrayList<HashMap<String, String>>();
        //HashMap<String, String> cadenaLineas = new HashMap<String, String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //decodificar id de usuario
        //Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        cadena = "%"+cadena+"%";
        arrayOperarios=this.getProDao().getProOrdenOperariosDisponibles(cadena, id_empresa);
        
        return arrayOperarios;
    }
    
    
    //obtiene datos para el autopletar
    @RequestMapping(value="/get_autocomplete_equipo.json", method = RequestMethod.POST)
    public @ResponseBody ArrayList<HashMap<String, String>> getProOrdenEquiposJson(
           @RequestParam(value="cadena", required=true) String cadena,
           @RequestParam(value="iu", required=true) String id_user_cod,
           Model modcel) {
        
        ArrayList<HashMap<String, String>> arrayEquipo = new ArrayList<HashMap<String, String>>();
        //HashMap<String, String> cadenaLineas = new HashMap<String, String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //decodificar id de usuario
        //Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        cadena = "%"+cadena+"%";
        arrayEquipo=this.getProDao().getProOrdenEquipoDisponible(cadena, id_empresa);
        
        return arrayEquipo;
    }
    
    
    //obtiene datos para el autopletar
    @RequestMapping(value="/get_autocomplete_equipoadicional.json", method = RequestMethod.POST)
    public @ResponseBody ArrayList<HashMap<String, String>> getProOrdenEquipoadicionalJson(
           @RequestParam(value="cadena", required=true) String cadena,
           @RequestParam(value="iu", required=true) String id_user_cod,
           Model modcel) {
        
        ArrayList<HashMap<String, String>> arrayEquipoAdicional = new ArrayList<HashMap<String, String>>();
        //HashMap<String, String> cadenaLineas = new HashMap<String, String>();
        HashMap<String, String> userDat = new HashMap<String, String>();
        
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        
        //decodificar id de usuario
        //Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user_cod));
        cadena = "%"+cadena+"%";
        arrayEquipoAdicional=this.getProDao().getProOrdenEquipoAdicionalDisponible(cadena, id_empresa);
        
        return arrayEquipoAdicional;
    }
    
    
    


    
    /*
    //localhost:8080/com.mycompany_Kemikal_war_1.0-SNAPSHOT/controllers/logasignarutas/getPdfRequisicion/id/ui/out.json
    //Genera pdf de formulacion de
    @RequestMapping(value = "/getPdfRequisicion/{id}/{iu}/out.json", method = RequestMethod.GET ) 
    public ModelAndView getGeneraPdfRequisicionJson(
                @PathVariable("id") String id_orden,
                @PathVariable("iu") String id_user,
                HttpServletRequest request, 
                HttpServletResponse response, 
                Model model)
            throws ServletException, IOException, URISyntaxException, DocumentException {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> datos = new HashMap<String, String>();
        ArrayList<HashMap<String, Object>> productos= new ArrayList<HashMap<String, Object>>();
        
        ArrayList<HashMap<String, String>> datos_orden = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> datosEncabezadoPie= new HashMap<String, String>();
        ArrayList<HashMap<String, Object>> lista_productos = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, String>> lista_procedimiento = new ArrayList<HashMap<String, String>>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        String rfc_empresa=this.getGralDao().getRfcEmpresaEmisora(id_empresa);
        String razon_social_empresa = this.getGralDao().getRazonSocialEmpresaEmisora(id_empresa);
        
        Integer app_selected_tmp = 300;
        datosEncabezadoPie.put("nombre_empresa_emisora", razon_social_empresa);
        datosEncabezadoPie.put("titulo_reporte", this.getGralDao().getTituloReporte(id_empresa, app_selected_tmp));
        datosEncabezadoPie.put("codigo1", this.getGralDao().getCodigo1Iso(id_empresa, app_selected_tmp));
        datosEncabezadoPie.put("codigo2", this.getGralDao().getCodigo2Iso(id_empresa, app_selected_tmp));
        
        //obtener el directorio temporal
        String dir_tmp = this.getGralDao().getTmpDir();
        
        File file_dir_tmp = new File(dir_tmp);
        System.out.println("Directorio temporal: "+file_dir_tmp.getCanonicalPath());
        
        
        datos_orden = this.getProDao().getProOrden_Datos(Integer.parseInt(id_orden));
        datos.put("fecha", TimeHelper.getFechaActualYMDH());
        
        datos.put("folio", datos_orden.get(0).get("folio"));
        datos.put("fecha_elavorar", datos_orden.get(0).get("fecha_elavorar"));
        datos.put("flujo", datos_orden.get(0).get("flujo"));
        datos.put("observaciones", datos_orden.get(0).get("observaciones"));
        
        //obtiene las facturas del periodo indicado
        productos = this.getProDao().getPro_DatosOrdenProduccionPdf(id_orden, String.valueOf(datos_orden.get(0).get("pro_proceso_id")));
        
        
        String file_name = "Requisucion_"+rfc_empresa+"_"+datos.get("folio") +".pdf";
        //ruta de archivo de salida
        String fileout = file_dir_tmp +"/"+  file_name;
        
        //instancia a la clase que construye el pdf del reporte de facturas
        PdfProOrdenRequisicion pdf = new PdfProOrdenRequisicion(datosEncabezadoPie, fileout,productos,datos);
        
        System.out.println("Recuperando archivo: " + fileout);
        File file = new File(fileout);
        if (file.exists()){
            int size = (int) file.length(); // Tama√±o del archivo
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            response.setBufferSize(size);
            response.setContentLength(size);
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition","attachment; filename=\"" + file.getCanonicalPath() +"\"");
            FileCopyUtils.copy(bis, response.getOutputStream());
            response.flushBuffer();
        }
        
        return null;
    }
    
    
    //localhost:8080/com.mycompany_Kemikal_war_1.0-SNAPSHOT/controllers/logasignarutas/getPdfProduccion/1/NQ==/out.json
    //Genera pdf de formulacion de
    @RequestMapping(value = "/getPdfTerminado/{id}/{iu}/out.json", method = RequestMethod.GET ) 
    public ModelAndView getGeneraPdfTerminadoJson(
                @PathVariable("id") String id_orden,
                @PathVariable("iu") String id_user,
                HttpServletRequest request, 
                HttpServletResponse response, 
                Model model)
            throws ServletException, IOException, URISyntaxException, DocumentException {
        
        HashMap<String, String> userDat = new HashMap<String, String>();
        HashMap<String, String> datos = new HashMap<String, String>();
        ArrayList<HashMap<String, Object>> productos= new ArrayList<HashMap<String, Object>>();
        
        ArrayList<HashMap<String, String>> datos_orden = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> datosEncabezadoPie= new HashMap<String, String>();
        ArrayList<HashMap<String, Object>> lista_productos = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, String>> lista_procedimiento = new ArrayList<HashMap<String, String>>();
        
        //decodificar id de usuario
        Integer id_usuario = Integer.parseInt(Base64Coder.decodeString(id_user));
        Integer app_selected = 93;//catalogo de preorden produccion
        
        userDat = this.getHomeDao().getUserById(id_usuario);
        Integer id_empresa = Integer.parseInt(userDat.get("empresa_id"));
        String rfc_empresa=this.getGralDao().getRfcEmpresaEmisora(id_empresa);
        String razon_social_empresa = this.getGralDao().getRazonSocialEmpresaEmisora(id_empresa);
        
        datosEncabezadoPie.put("nombre_empresa_emisora", razon_social_empresa);
        datosEncabezadoPie.put("titulo_reporte", this.getGralDao().getTituloReporte(id_empresa, app_selected));
        datosEncabezadoPie.put("codigo1", this.getGralDao().getCodigo1Iso(id_empresa, app_selected));
        datosEncabezadoPie.put("codigo2", this.getGralDao().getCodigo2Iso(id_empresa, app_selected));
        
        //obtener el directorio temporal
        String dir_tmp = this.getGralDao().getTmpDir();
        
        File file_dir_tmp = new File(dir_tmp);
        System.out.println("Directorio temporal: "+file_dir_tmp.getCanonicalPath());
        
        datos_orden = this.getProDao().getProOrden_Datos(Integer.parseInt(id_orden));
        datos.put("fecha", TimeHelper.getFechaActualYMDH());
        
        datos.put("folio", datos_orden.get(0).get("folio"));
        datos.put("fecha_elavorar", datos_orden.get(0).get("fecha_elavorar"));
        datos.put("flujo", datos_orden.get(0).get("flujo"));
        datos.put("observaciones", datos_orden.get(0).get("observaciones"));
        
        //obtiene las facturas del periodo indicado
        productos = this.getProDao().getPro_DatosOrdenProduccionPdf(id_orden, String.valueOf(datos_orden.get(0).get("pro_proceso_id")));
        
        
        String file_name = "PRODUCCION_"+rfc_empresa+"_"+datos.get("folio") +".pdf";
        //ruta de archivo de salida
        String fileout = file_dir_tmp +"/"+  file_name;
        
        //instancia a la clase que construye el pdf del reporte de facturas
        PdfProOrdenProduccion pdf = new PdfProOrdenProduccion(datosEncabezadoPie, fileout,productos,datos);
        
        System.out.println("Recuperando archivo: " + fileout);
        File file = new File(fileout);
        if (file.exists()){
            int size = (int) file.length(); // Tama√±o del archivo
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            response.setBufferSize(size);
            response.setContentLength(size);
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition","attachment; filename=\"" + file.getCanonicalPath() +"\"");
            FileCopyUtils.copy(bis, response.getOutputStream());
            response.flushBuffer();
        }
        
        return null;
    }
    */
    
    
    
}
