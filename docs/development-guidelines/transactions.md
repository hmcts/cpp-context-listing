Here's how to debug transactions in IntelliJ IDEA:

  ────────────────────────────────────────



  1. See How Annotations Affect Transactions

  Your project uses two different transaction annotation styles:

  ┌───────────────────────────────┬───────────────────────────────────────────────────────────────┬───────────────────────────────────────────────┐
  │ Annotation                    │ Used in                                                       │ Behavior                                      │
  ├───────────────────────────────┼───────────────────────────────────────────────────────────────┼───────────────────────────────────────────────┤
  │ @TransactionAttribute(REQUIRE │ CourtScheduleRetryService                                     │ Container starts a new TX, suspends any       │
  │ S_NEW)                        │                                                               │ existing one                                  │
  │ (EJB)                         │                                                               │                                               │
  │ @Transactional(REQUIRES_NEW)  │ RotaFilePartialProcessor, JudiciaryAssignmentService,         │ Same as above, but via CDI interceptor        │
  │ (JTA)                         │ JudiciaryUnassignmentService                                  │                                               │
  │ @Transactional (no qualifier) │ SessionsService, CourtScheduleRepository                      │ Uses REQUIRED (default) -- joins existing TX  │
  │                               │                                                               │ or creates new one                            │
  │ @TransactionAttribute(NOT_SUP │ RotaFileProcessorService                                      │ Suspends any existing TX, runs without one    │
  │ PORTED)                       │                                                               │                                               │
  └───────────────────────────────┴───────────────────────────────────────────────────────────────┴───────────────────────────────────────────────┘

  To debug which path is taken in IntelliJ:
  Set breakpoints on the transaction interceptor/proxy classes:
  • Run > Debug Configurations -- connect to your WildFly remote debug port (default 8787)
  • Set breakpoints in these framework classes (use Ctrl+N to open):
    • com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple -- the actual Narayana/Arjuna TX implementation in WildFly
    • com.arjuna.ats.jta.UserTransaction -- for begin()/commit()/rollback()
  • Or more practically, set method breakpoints on your annotated methods and check the call stack to see the EJB/CDI interceptor
    creating/joining transactions.



  2. See Which Transaction Is Active at a Breakpoint

  When stopped at any breakpoint, use IntelliJ's Evaluate Expression (Alt+F8) to inspect the active transaction:

  // Get the current transaction status
  com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction()


  // Check transaction status (0 = STATUS_ACTIVE, 1 = STATUS_MARKED_ROLLBACK, etc.)
  com.arjuna.ats.jta.TransactionManager.transactionManager().getStatus()


  // Get the transaction ID (Arjuna UID)
  com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction().toString()

  You can also evaluate:

  // Check if EntityManager is joined to a TX
  entityManager.isJoinedToTransaction()

  Tip: Add these as watches in the Variables/Watches panel so they update automatically at every breakpoint.


  3. See Actual SQL Queries with Parameter Values



  Option A: Enable Hibernate SQL Logging (Recommended)

  Your persistence.xml already has hibernate.show_sql set to false. To see queries with parameters, change these properties temporarily:

  <property name="hibernate.show_sql" value="true"/>
  <property name="hibernate.format_sql" value="true"/>
  <property name="hibernate.use_sql_comments" value="true"/>

  But show_sql only prints queries without bind parameter values. To see parameter values, you need to set the Hibernate log level. Add this to
  your WildFly standalone.xml (or via CLI):

  <logger category="org.hibernate.SQL">
      <level name="DEBUG"/>
  </logger>
  <logger category="org.hibernate.type.descriptor.sql.BasicBinder">
      <level name="TRACE"/>
  </logger>

  Or via WildFly CLI:

  /subsystem=logging/logger=org.hibernate.SQL:add(level=DEBUG)
  /subsystem=logging/logger=org.hibernate.type.descriptor.sql.BasicBinder:add(level=TRACE)

  This will log output like:

  DEBUG org.hibernate.SQL - insert into court_schedule (id, ...) values (?, ?, ?)
  TRACE o.h.t.d.s.BasicBinder - binding parameter [1] as [VARCHAR] - [abc-123]
  TRACE o.h.t.d.s.BasicBinder - binding parameter [2] as [DATE] - [2026-03-01]



  Option B: Use a JDBC Proxy (P6Spy / Datasource Proxy)

  For the cleanest view of all queries with inline parameter values, add p6spy as a JDBC wrapper. But this requires modifying the datasource config
   on WildFly, so Option A is usually easier.


  Option C: IntelliJ Database Console + Breakpoint Combo

  1. Connect IntelliJ's Database tool (View > Tool Windows > Database) to your database
  2. Run the query SELECT * FROM pg_stat_activity (PostgreSQL) or equivalent to see live queries while you're paused at a breakpoint



  4. Practical Debugging Workflow

  Here's a step-by-step workflow for a typical scenario, e.g. debugging RotaFilePartialProcessor.processFullRotaFile():
  1. Start WildFly in debug mode (add -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8787 to JAVA_OPTS)
  2. In IntelliJ, create a Remote JVM Debug run configuration pointing to localhost:8787
  3. Set a breakpoint on line 72-73 of RotaFilePartialProcessor (the @Transactional(REQUIRES_NEW) method)
  4. When it hits, check the Frames panel -- you'll see the CDI transaction interceptor in the call stack
  5. In Evaluate Expression, run:


     com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction().toString()

     This gives you the Arjuna TX ID. Note it down.
  6. Step into sessionsService.updateSlotsAndSchedules() (which has @Transactional = REQUIRED) and evaluate the same expression -- you'll see the
     same TX ID, confirming it joined the existing transaction
  7. Step into courtScheduleRetryService.upsertOne() (which has @TransactionAttribute(REQUIRES_NEW)) -- you'll see a different TX ID, confirming
     a new transaction was created

  This way you can map exactly how the annotation semantics play out across your service call chain.
