package com.kafka.application.domain

import java.time.LocalDate
import io.circe.derivation.Configuration
import io.circe.derivation.ConfiguredEncoder

given Configuration = Configuration.default.withDefaults

final case class Person(
    _id: String,
    name: String,
    dob: LocalDate,
    address: Address,
    telephone: String,
    pets: List[String],
    score: Double,
    email: String,
    url: String,
    description: String,
    verified: Boolean,
    salary: Int
) derives ConfiguredEncoder

final case class Address(
    street: String,
    town: String,
    postode: String
) derives ConfiguredEncoder

final case class People(people: List[Person]) derives ConfiguredEncoder

