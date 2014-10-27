package fi.vm.sade.ldap

import com.unboundid.ldap.sdk.{SearchScope, SearchRequest, Filter, LDAPConnection}
import fi.vm.sade.valintatulosservice.config.AppConfig.AppConfig
import org.json4s.DefaultFormats

class LdapClient(config: LdapConfig) {
  import collection.JavaConversions._
  implicit val formats = DefaultFormats

  def findUser(userid: String) = {
    val connection: LDAPConnection = new LDAPConnection()
    connection.connect(config.host, 389)
    try {
      connection.bind(config.userDn, config.password)
      val filter = Filter.createEqualityFilter("uid", userid);
      val searchRequest =
        new SearchRequest("ou=People,dc=opintopolku,dc=fi", SearchScope.SUB, filter, "description", "uid");
      connection.search(searchRequest).getSearchEntries.toList.headOption.map { result =>
        val roles = Option(result.getAttribute("description").getValue).toList.flatMap { crazyString =>
          org.json4s.jackson.parseJson(crazyString).extract[List[String]]
        }
        LdapUser(roles)
      }
    } finally {
      connection.close
    }
  }
}