package it.pagopa.pdnd.interop.uservice.authorizationprocess.model


/**
 * @param detail A human readable explanation specific to this occurrence of the problem. for example: ''Request took too long to complete.''
 * @param status The HTTP status code generated by the origin server for this occurrence of the problem. for example: ''503''
 * @param title A short, summary of the problem type. Written in english and readable for example: ''Service Unavailable''
*/
final case class Problem (
  detail: Option[String],
  status: Int,
  title: String
)

