# YSQL write workload with range-based pk/index

##  Workload Descriptions

- insert_varying_composite_index.yaml / update_varying_composite_index.yaml:
  Use a varying number of range-based columns in a composite index.

- insert_varying_index.yaml / update_varying_index.yaml:
  Use a varying number of range-based indexes (non-composite).

- insert_varying_pk_columns.yaml / update_varying_pk_columns.yaml:
  Use a varying number of range-based columns in a primary key.

- multi_statement_transaction.yaml:
  A test that performs a combination of `INSERT` and `UPDATE` operations within a single transaction.


