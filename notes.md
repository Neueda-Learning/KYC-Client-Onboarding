Lombok @RequiredArgsContrsuctor automatically writes a constructor for every filed maked final

@Component is a generic parent annotation. When a class doesnt fit specific web/bussiness layer (like utility helper or background job)

@Service - bussiness logic classes

@RestController - handle incoming web requests (POST, GET, etc) and return data directly yo a client

@Entity -Java class representation of database record -> map class to db

@Table(name="users") specifies name of table in db. if not set, defaults to class name (JPA will know its a table bc of @Entity)

@Repository - marks interface or class responsible for database operations

@Id @GeneratedValue - its primary key and generate increment automatically


Autoamtic Spring questies:
1. Action Prefix : keywords like findBy, readBy, countBy, deleteBy
2. Property name: match names in entity like Name, Email, CreatedAt
3. Conditions and operators: combines criteria using keywords : And, Or, Between, LessThan, Contraining, IgnoreCase. 
findByNameAndEmail will be SELECT * FROM customers where name=? and email=?


Lombok:
@Data - craetes getters, setters, equals(), hashCode(), toString() behind scenes

@NoArgsConstructor - generates empty constructor with no parameters. JPA requires epty constructor to create objects from db records

@AllArgsConstructor - generates contructor that uses every field as argument

@Builder - Create objects cleanly w/o list of constructor arguments. 

@slf4j - injects a logge automatically

