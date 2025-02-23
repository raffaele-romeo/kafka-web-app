package com.kafka.application.domain

import io.circe.Json

final case class ListJson(ctRoot: List[Json])
