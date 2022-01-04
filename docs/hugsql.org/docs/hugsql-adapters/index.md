---
sidebar_position: 0
---

# HugSQL Adapters

One of HugSQL's design goals is to balance the coupling of an SQL-template library (itself) with an underlying database library of the developer's choosing. We could concede to no coupling at all--providing only `def-sqlvec-fns`. However, wrapping a few underlying database library functions in a protocol provides a nice easy-path for most use cases. And, where necessary to use the underlying database libraries directly, HugSQL attempts to stay out of the way and give you the tools to do what you want to do.

In the spirit of the above thoughts, HugSQL provides an adapter protocol to allow your choice of underlying database library.
