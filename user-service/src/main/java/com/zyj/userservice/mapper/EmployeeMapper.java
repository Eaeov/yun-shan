package com.zyj.userservice.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import com.zyj.userservice.annotation.AutoFill;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 根据用户名查询员工
     * <p>登录时调用，此时 AuthContext 中无员工信息，必须跳过租户插件过滤，
     * 否则 SQL 会被追加 merchant_id 条件导致查不到任何记录。</p>
     * @param username 用户名
     * @return 员工对象，不存在则返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(@Param("username") String username);

    @AutoFill(value = OperationType.INSERT)
    @Insert("insert into employee(name,username,password,phone,sex,id_number,status,merchant_id,role,create_time,update_time,create_user,update_user)" +
            " values " +
            "(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{merchantId},#{role},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    int insert(Employee employee);
    /**
     * 根据id查询员工
     * <p>修改员工信息时调用，此时 AuthContext 中无员工信息，必须跳过租户插件过滤，
     * 否则 SQL 会被追加 merchant_id 条件导致查不到任何记录。</p>
     * @param id 员工id
     * @return 员工对象
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);

    List<Employee> list(EmployeePageQueryDTO employeePageQueryDTO);
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee build);
}
