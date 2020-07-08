package org.apache.jmeter.protocol.http.visualizers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.gui.util.HeaderAsPropertyRenderer;
import org.apache.jmeter.gui.util.TextBoxDialoger.TextBoxDoubleClick;
import org.apache.jmeter.protocol.http.config.MultipartUrlConfig;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.RequestView;
import org.apache.jmeter.visualizers.SamplerResultTab.RowResult;
import org.apache.jmeter.visualizers.SearchTextExtension;
import org.apache.jmeter.visualizers.SearchTextExtension.ISearchTextExtensionProvider;
import org.apache.jorphan.gui.GuiUtils;
import org.apache.jorphan.gui.ObjectTableModel;
import org.apache.jorphan.gui.RendererUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.Functor;
import org.apache.log.Logger;

public class RequestViewHTTP
  implements RequestView
{
  private static final Logger log = LoggingManager.getLoggerForClass();
  private static final String KEY_LABEL = "view_results_table_request_tab_http";
  private static final String CHARSET_DECODE = utf-8;
  private static final String PARAM_CONCATENATE = "&";
  private JPanel paneParsed;
  private ObjectTableModel requestModel = null;

  private ObjectTableModel paramsModel = null;

  private ObjectTableModel headersModel = null;

  private static final String[] COLUMNS_REQUEST = { " ", " " };

  private static final String[] COLUMNS_PARAMS = { "view_results_table_request_params_key", "view_results_table_request_params_value" };

  private static final String[] COLUMNS_HEADERS = { "view_results_table_request_headers_key", "view_results_table_request_headers_value" };

  private JTable tableRequest = null;

  private JTable tableParams = null;

  private JTable tableHeaders = null;

  private static final TableCellRenderer[] RENDERERS_REQUEST = { null, null };

  private static final TableCellRenderer[] RENDERERS_PARAMS = { null, null };

  private static final TableCellRenderer[] RENDERERS_HEADERS = { null, null };
  private SearchTextExtension searchTextExtension;

  public RequestViewHTTP()
  {
    this.requestModel = new ObjectTableModel(COLUMNS_REQUEST, SamplerResultTab.RowResult.class, new Functor[] { new Functor("getKey"), new Functor("getValue") }, new Functor[] { null, null }, new Class[] { String.class, String.class }, false);

    this.paramsModel = new ObjectTableModel(COLUMNS_PARAMS, SamplerResultTab.RowResult.class, new Functor[] { new Functor("getKey"), new Functor("getValue") }, new Functor[] { null, null }, new Class[] { String.class, String.class }, false);

    this.headersModel = new ObjectTableModel(COLUMNS_HEADERS, SamplerResultTab.RowResult.class, new Functor[] { new Functor("getKey"), new Functor("getValue") }, new Functor[] { null, null }, new Class[] { String.class, String.class }, false);
  }

  public void init()
  {
    this.paneParsed = new JPanel(new BorderLayout(0, 5));
    this.paneParsed.add(createRequestPane());
    this.searchTextExtension = new SearchTextExtension();
    this.searchTextExtension.init(this.paneParsed);
    JPanel searchPanel = this.searchTextExtension.createSearchTextExtensionPane();
    searchPanel.setBorder(null);
    this.searchTextExtension.setSearchProvider(new RequestViewHttpSearchProvider(null));
    searchPanel.setVisible(true);
    this.paneParsed.add(searchPanel, "Last");
  }

  public void clearData()
  {
    this.requestModel.clearData();
    this.paramsModel.clearData();
    this.headersModel.clearData();
  }

  public void setSamplerResult(Object objectResult)
  {
    this.searchTextExtension.resetTextToFind();
    if ((objectResult instanceof HTTPSampleResult)) {
      HTTPSampleResult sampleResult = (HTTPSampleResult)objectResult;

      this.requestModel.addRow(new SamplerResultTab.RowResult(JMeterUtils.getResString("view_results_table_request_http_method"), sampleResult.getHTTPMethod()));

      LinkedHashMap lhm = JMeterUtils.parseHeaders(sampleResult.getRequestHeaders());
      for (Map.Entry entry : lhm.entrySet()) {
        this.headersModel.addRow(new SamplerResultTab.RowResult((String)entry.getKey(), entry.getValue()));
      }

      URL hUrl = sampleResult.getURL();
      if (hUrl != null) {
        this.requestModel.addRow(new SamplerResultTab.RowResult(JMeterUtils.getResString("view_results_table_request_http_protocol"), hUrl.getProtocol()));

        this.requestModel.addRow(new SamplerResultTab.RowResult(JMeterUtils.getResString("view_results_table_request_http_host"), hUrl.getHost()));

        int port = hUrl.getPort() == -1 ? hUrl.getDefaultPort() : hUrl.getPort();
        this.requestModel.addRow(new SamplerResultTab.RowResult(JMeterUtils.getResString("view_results_table_request_http_port"), Integer.valueOf(port)));

        this.requestModel.addRow(new SamplerResultTab.RowResult(JMeterUtils.getResString("view_results_table_request_http_path"), hUrl.getPath()));

        String queryGet = hUrl.getQuery() == null ? "" : hUrl.getQuery();
        boolean isMultipart = isMultipart(lhm);

        String queryPost = sampleResult.getQueryString();
        if ((!isMultipart) && (StringUtils.isNotBlank(queryPost))) {
          if (queryGet.length() > 0) {
            queryGet = queryGet + "&";
          }
          queryGet = queryGet + queryPost;
        }

        if (StringUtils.isNotBlank(queryGet)) {
          Set keys = getQueryMap(queryGet).entrySet();
          for (Map.Entry entry : keys) {
            for (String value : (String[])entry.getValue()) {
              this.paramsModel.addRow(new SamplerResultTab.RowResult((String)entry.getKey(), value));
            }
          }
        }

        if ((isMultipart) && (StringUtils.isNotBlank(queryPost))) {
          String contentType = (String)lhm.get("Content-Type");
          String boundaryString = extractBoundary(contentType);
          MultipartUrlConfig urlconfig = new MultipartUrlConfig(boundaryString);
          urlconfig.parseArguments(queryPost);

          for (JMeterProperty prop : urlconfig.getArguments()) {
            Argument arg = (Argument)prop.getObjectValue();
            this.paramsModel.addRow(new SamplerResultTab.RowResult(arg.getName(), arg.getValue()));
          }
        }

      }

      String cookie = sampleResult.getCookies();
      if ((cookie != null) && (cookie.length() > 0)) {
        this.headersModel.addRow(new SamplerResultTab.RowResult(JMeterUtils.getParsedLabel("view_results_table_request_http_cookie"), sampleResult.getCookies()));
      }

    }
    else
    {
      this.requestModel.addRow(new SamplerResultTab.RowResult("", JMeterUtils.getResString("view_results_table_request_http_nohttp")));
    }
  }

  private String extractBoundary(String contentType)
  {
    String boundaryString = contentType.substring(contentType.toLowerCase(Locale.ENGLISH).indexOf("boundary=") + "boundary=".length());

    String[] split = boundaryString.split(";");
    if (split.length > 1) {
      boundaryString = split[0];
    }
    return boundaryString;
  }

  private boolean isMultipart(LinkedHashMap<String, String> headers)
  {
    String contentType = (String)headers.get("Content-Type");
    if ((contentType != null) && (contentType.startsWith("multipart/form-data"))) {
      return true;
    }
    return false;
  }

  public static Map<String, String[]> getQueryMap(String query)
  {
    Map map = new HashMap();
    String[] params = query.split("&");
    for (String param : params) {
      String[] paramSplit = param.split("=");
      String name = decodeQuery(paramSplit[0]);

      if (name.trim().startsWith("<?")) {
        map.put(" ", new String[] { query });
        return map;
      }

      if (((param.startsWith("=")) && (paramSplit.length == 1)) || (paramSplit.length > 2)) {
        map.put(" ", new String[] { query });
        return map;
      }

      String value = "";
      if (paramSplit.length > 1) {
        value = decodeQuery(paramSplit[1]);
      }

      String[] known = (String[])map.get(name);
      if (known == null) {
        known = new String[] { value };
      }
      else {
        String[] tmp = new String[known.length + 1];
        tmp[(tmp.length - 1)] = value;
        System.arraycopy(known, 0, tmp, 0, known.length);
        known = tmp;
      }
      map.put(name, known);
    }

    return map;
  }

  public static String decodeQuery(String query)
  {
    if ((query != null) && (query.length() > 0)) {
      try {
        return URLDecoder.decode(query, CHARSET_DECODE);
      } catch (IllegalArgumentException|UnsupportedEncodingException e) {
        log.warn("Error decoding query, maybe your request parameters should be encoded:" + query, e);

        return query;
      }
    }
    return "";
  }

  public JPanel getPanel()
  {
    return this.paneParsed;
  }

  private Component createRequestPane()
  {
    this.tableRequest = new JTable(this.requestModel);
    JMeterUtils.applyHiDPI(this.tableRequest);
    this.tableRequest.setToolTipText(JMeterUtils.getResString("textbox_tooltip_cell"));
    this.tableRequest.addMouseListener(new TextBoxDialoger.TextBoxDoubleClick(this.tableRequest));

    setFirstColumnPreferredAndMaxWidth(this.tableRequest);
    RendererUtils.applyRenderers(this.tableRequest, RENDERERS_REQUEST);

    this.tableParams = new JTable(this.paramsModel);
    JMeterUtils.applyHiDPI(this.tableParams);
    this.tableParams.setToolTipText(JMeterUtils.getResString("textbox_tooltip_cell"));
    this.tableParams.addMouseListener(new TextBoxDialoger.TextBoxDoubleClick(this.tableParams));
    TableColumn column = this.tableParams.getColumnModel().getColumn(0);
    column.setPreferredWidth(160);
    this.tableParams.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());
    RendererUtils.applyRenderers(this.tableParams, RENDERERS_PARAMS);

    this.tableHeaders = new JTable(this.headersModel);
    JMeterUtils.applyHiDPI(this.tableHeaders);
    this.tableHeaders.setToolTipText(JMeterUtils.getResString("textbox_tooltip_cell"));
    this.tableHeaders.addMouseListener(new TextBoxDialoger.TextBoxDoubleClick(this.tableHeaders));
    setFirstColumnPreferredAndMaxWidth(this.tableHeaders);
    this.tableHeaders.getTableHeader().setDefaultRenderer(new HeaderAsPropertyRenderer());

    RendererUtils.applyRenderers(this.tableHeaders, RENDERERS_HEADERS);

    JSplitPane topSplit = new JSplitPane(0, GuiUtils.makeScrollPane(this.tableParams), GuiUtils.makeScrollPane(this.tableHeaders));

    topSplit.setOneTouchExpandable(true);
    topSplit.setResizeWeight(0.5D);
    topSplit.setBorder(null);

    JSplitPane paneParsed = new JSplitPane(0, GuiUtils.makeScrollPane(this.tableRequest), topSplit);

    paneParsed.setOneTouchExpandable(true);
    paneParsed.setResizeWeight(0.25D);
    paneParsed.setBorder(null);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(paneParsed);
    return panel;
  }

  private void setFirstColumnPreferredAndMaxWidth(JTable table) {
    TableColumn column = table.getColumnModel().getColumn(0);
    column.setMaxWidth(300);
    column.setPreferredWidth(160);
  }

  public String getLabel()
  {
    return JMeterUtils.getResString("view_results_table_request_tab_http");
  }

  private class RequestViewHttpSearchProvider
    implements SearchTextExtension.ISearchTextExtensionProvider
  {
    private int lastPosition = -1;

    private RequestViewHttpSearchProvider() {
    }
    public void resetTextToFind() { this.lastPosition = -1;
      if (RequestViewHTTP.this.tableParams != null)
        RequestViewHTTP.this.tableParams.clearSelection();
    }

    public boolean executeAndShowTextFind(Pattern pattern)
    {
      boolean found = false;
      if (RequestViewHTTP.this.tableParams != null) {
        RequestViewHTTP.this.tableParams.clearSelection();

        for (int i = this.lastPosition + 1; i < RequestViewHTTP.this.tableParams.getRowCount(); i++) {
          for (int j = 0; j < RequestViewHTTP.COLUMNS_PARAMS.length; j++) {
            Object o = RequestViewHTTP.this.tableParams.getModel().getValueAt(i, j);
            if ((o instanceof String)) {
              Matcher matcher = pattern.matcher((String)o);
              if ((matcher != null) && (matcher.find())) {
                found = true;
                RequestViewHTTP.this.tableParams.setRowSelectionInterval(i, i);
                RequestViewHTTP.this.tableParams.scrollRectToVisible(RequestViewHTTP.this.tableParams.getCellRect(i, 0, true));
                this.lastPosition = i;
                break label164;
              }
            }
          }
        }

        label164: if (!found) {
          resetTextToFind();
        }
      }
      return found;
    }
  }
}
