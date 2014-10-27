package fi.vm.sade.security.ldap

case class LdapUser(roles: List[String]) {
  def hasRole(role: String) = {
    roles.contains(role)
  }
}
