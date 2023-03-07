<%--
  Renders a List of items for ordering purposes only
--%><%
%><%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
      import="java.util.Iterator,
        com.adobe.granite.ui.components.AttrBuilder,
        com.adobe.granite.ui.components.Tag,
        com.adobe.granite.ui.components.Config,
        org.apache.sling.api.resource.ValueMap,
        java.util.stream.StreamSupport,
        java.util.stream.Collectors,
        java.util.*" %>
<%--###
  Ordered List
  ====
  // TODO
  .. granite:servercomponent:: widgets/orderedList

    Renders a List of items for ordering purposes only
    
    It has the following content structure:
    + myList
      - sling:resourceType = "widgets/orderedList"
      - name = "./myList"
      - title = "My List"
      + items
        + item1
          - text = "Item 1"
          - value = "item1"
        + item 2
          - text = "Item 2"
          - value = "item2"
###--%>

<%
  if (!cmp.getRenderCondition(resource, false).check()) {
    return;
  }
  Tag tag = cmp.consumeTag();
  AttrBuilder attrs = tag.getAttrs();
  attrs.addClass("coral-Well");
  cmp.populateCommonAttrs(attrs);
  Config cfg = cmp.getConfig();
  String title = cfg.get("title", String.class);
  String name = cfg.get("name", String.class);
  
  
  String cleanName = 
    name != null 
    ? name.replace(".", "").replace("/", "")
    : "";
  String tableId = "order-table-" + cleanName;
  String hiddenInputId = "order-input-" + cleanName;

  String[] values = cmp.getValue().getContentValue(name, new String[0]);
  List<String> valuesList = Arrays.asList(values);
  Iterator<Resource> itemsIterator = cmp.getItemDataSource().iterator();
  
  
  List<ValueMap> items = StreamSupport.stream(
      Spliterators.spliteratorUnknownSize(itemsIterator, Spliterator.ORDERED),
      false
    ).sorted(
      Comparator.comparing(item -> {
        ValueMap vm = ((Resource) item).getValueMap();
        String val = vm.get("value", "");
        return valuesList != null ? valuesList.indexOf(val) : 0;
      })
    ).map(Resource::getValueMap)
    .collect(Collectors.toList());
%>
<div <%= attrs.build() %>>
  <table is="coral-table" orderable id="<%=tableId%>">
    <colgroup>
      <col is="coral-table-column">
      <col is="coral-table-column" fixedwidth>
    </colgroup>
    <thead is="coral-table-head">
      <tr is="coral-table-row">
        <th is="coral-table-headercell"><%=title%></th>
        <th is="coral-table-headercell"></th>
      </tr>
    </thead>
    <tbody is="coral-table-body">
      <%
      for (ValueMap vm : items) {
      %>
        <tr is="coral-table-row">
          <td is="coral-table-cell">
            <%=vm.get("text", "") %>
            <input type="hidden" name="<%=name%>" value="<%=vm.get("value", "")%>"/>
          </td>
          <td is="coral-table-cell">
            <button is="coral-button" type="button" variant="minimal" icon="dragHandle" coral-table-roworder></button>
          </td>
        </tr>
      <%
      }
      %>
    </tbody>
  </table>
</div>