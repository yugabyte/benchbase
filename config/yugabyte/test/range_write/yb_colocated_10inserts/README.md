# YSQL write workload with range-based pk/index

##  Workload Descriptions
repeatable read isolation is used in these workloads.
- RW_G1_insert_varying_composite_index.yaml:
  <br> The workload inserts a row to a table with a composite index. Use a varying number of range-based columns in a composite index.

- RW_G2_insert_varying_index.yaml:
  <br> The workload inserts a row to a table with some indexes. Use a varying number of range-based indexes (non-composite).

- RW_G3_insert_varying_pk_columns.yaml:
  <br> The workload inserts a row to a table with range-based primary key. Use a varying number of range-based columns in a primary key.

- RW_G4_multi_statement_transaction.yaml:
  <br> The workload performs a combination of `INSERT` and `UPDATE` operations within a single transaction.

- RW_G5_update_varying_composite_index.yaml:
  <br> The workload updates a row in a table with a composite index. Use a varying number of range-based columns in a composite index.

- RW_G6_update_varying_index.yaml:
  <br> The workload updates a row to in table with some indexes. Use a varying number of range-based indexes (non-composite).

- RW_G7_update_varying_pk_columns.yaml:
  <br> The workload updates a row in a table with range-based primary key. Use a varying number of range-based columns in a primary key.
