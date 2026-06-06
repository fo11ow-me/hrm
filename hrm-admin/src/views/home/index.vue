<template>
  <div>
    <el-row class="home" :gutter="20">
      <el-col :span="8" style="margin-top: 20px">
        <el-card shadow="hover">
          <div class="user">

            <el-upload style="margin-bottom: -120px;margin-right: -10px"
                       :action="uploadApi" :headers="headers" :show-file-list="false" :multiple="false"
                       :on-success="handleUploadSuccess"
                       :limit="1">
              <i class="el-icon-edit"></i>
            </el-upload>
            <img ref="img" src="" alt="头像"/>
            <div class="userinfo">
              <p style="margin-bottom: 15px;font-weight: bold;">{{ currentDateInfo }}</p>
              <p class="name">{{ staff.name }}</p>
            </div>
          </div>
          <div class="login-info">
            <p>生日：<span>{{ staff.birthday }}</span></p>
            <p>地址：<span>{{ staff.address }}</span></p>
            <p>部门：<span>{{ staff.deptName }}</span></p>
          </div>
        </el-card>
        <el-card style="margin-top: 20px;height:487px">
          <el-calendar v-model="attendanceData.date">
            <template
              slot="dateCell"
              slot-scope="{date, data}">
              <span>{{ date.getDate() }}</span>
              <!-- 使用v-if避免在没有数据的时候显示解析-->
              <el-tag v-show="data.type==='current-month'" v-if="attendanceData.list[date.getDate() - 1]"
                      :type="attendanceData.list[date.getDate() - 1].tagType">
                {{ attendanceData.list[date.getDate() - 1].message }}
              </el-tag>
            </template>
          </el-calendar>
        </el-card>
      </el-col>
      <el-col :span="16" style="margin-top: 20px">
        <div class="num">
          <el-card v-for="(item,index) in countData" :key="index" :body-style="{ display: 'flex', padding: 0 }">
            <i class="icon" :class="'el-icon-' +item.icon" :style="{ background: item.color }"/>
            <div class="detail">
              <p class="txt" style=" margin-bottom: 5px">{{ item.name }}</p>
              <p class="num">{{ item.value }}</p>
            </div>
          </el-card>
        </div>
        <el-card style="height: 300px">
          <div style="height: 300px" ref="city"></div>
        </el-card>
        <div class="graph">
          <el-card style="height: 300px">
            <div style="height: 300px" ref="staff"></div>
          </el-card>
          <el-card style="height: 300px">
            <div style="height: 300px" ref="department"></div>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script>
import * as echarts from 'echarts'
import { queryCount, queryCity, queryStaff, queryAttendance, queryDepartment } from '@/api/home'
import { getUploadApi } from '@/api/docs'
import { edit, queryInfo } from '@/api/staff'
import { setAvatar } from '@/utils/avatar'
import { mapGetters } from 'vuex'

export default {
  name: 'Home',
  data () {
    return {
      dayOfWeek: ['星期天', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
      attendanceData: {
        date: '',
        list: []
      },
      countData: [
        {
          name: '员工总数',
          value: 0,
          icon: 'user',
          color: '#409EFF'
        },
        {
          name: '状态正常',
          value: 0,
          icon: 'star-on',
          color: '#67C23A'
        },
        {
          name: '状态异常',
          value: 0,
          icon: 's-goods',
          color: '#F56C6C'
        },
        {
          name: '今日迟到',
          value: 0,
          icon: 'timer',
          color: '#409EFF'
        },
        {
          name: '今日早退',
          value: 0,
          icon: 'watch',
          color: '#67C23A'
        },
        {
          name: '今日旷工',
          value: 0,
          icon: 'lock',
          color: '#F56C6C'
        }
      ],
      staffOption: {
        title: {
          text: new Date().getFullYear() + '年员工入职情况',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}人',
          textStyle: {
            fontWeight: 'bold'
          }
        },
        xAxis: {
          type: 'category',
          data: []
        },
        yAxis: {
          type: 'value',
          name: '人数'
        },
        series: [
          {
            data: [],
            type: 'line'
          },
          {
            data: [],
            type: 'bar'
          }
        ]
      },
      cityOption: {
        title: {
          text: '部分城市社保缴费比例'
        },
        legend: {},
        tooltip: {
          trigger: 'item',
          formatter: function (params) {
            return '<a>' + params.name + '</a></a>' + '<br /><a>' + params.seriesName + ': ' + params.data[params.seriesIndex + 1] * 100 + '%</a>'
          },
          textStyle: {
            fontWeight: 'bold'
          }
        },
        dataset: {
          source: []
        },
        xAxis: { type: 'category' },
        yAxis: {
          type: 'value',
          name: '百分比',
          axisLabel: {
            formatter: function (val) {
              return val * 100 + '%'
            }
          }
        },
        series: [{ type: 'line' }, { type: 'line' }, { type: 'line' }, { type: 'line' }]
      },
      departmentOption: {
        title: {
          text: '员工分布情况',
          left: 'center'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}人',
          textStyle: {
            fontWeight: 'bold'
          }
        },
        toolbox: {
          show: true,
          feature: {
            mark: { show: true },
            dataView: { show: true, readOnly: false },
            restore: { show: true },
            saveAsImage: { show: true }
          }
        },
        series: [
          {
            name: 'Nightingale Chart',
            type: 'pie',
            radius: ['10%', '60%'],
            center: ['50%', '55%'],
            roseType: 'area',
            label: { // 饼图图形上的文本标签
              normal: {
                show: true,
                position: 'inner', // 标签的位置
                textStyle: {
                  fontWeight: 'bold'
                },
                formatter: '{d}%'
              }
            },
            itemStyle: {
              borderRadius: 3
            },
            data: []
          }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['staff', 'token']),
    headers () {
      return { Authorization: 'Bearer ' + this.token }
    },
    uploadApi () {
      return getUploadApi(this.staff.id)
    },
    currentDateInfo () {
      const date = new Date()
      const year = date.getFullYear()
      const month = date.getMonth() + 1
      const currentDate = date.getDate()
      const day = date.getDay()
      return year + '年' + month + '月' + currentDate + '日' + ' ' + this.dayOfWeek[day]
    }
  },
  watch: {
    'attendanceData.date':
      function () {
        this.getStaffAttendance()
      },
    'staff.avatar': // 当头像被修改时，重新获取
      function () {
        setAvatar(this.$refs.img)
      }
  },
  mounted () {
    setAvatar(this.$refs.img)
  },
  methods: {
    loading () {
      queryInfo(this.staff.id).then(response => {
        if (response.code === 200) {
          this.$store.commit('staff/SET_STAFF', response.data)
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      queryStaff().then(response => {
        if (response.code === 200) {
          const quarters = ['一季度', '二季度', '三季度', '四季度']
          this.staffOption.xAxis.data = quarters
          this.staffOption.series.forEach(item => {
            item.data = response.data
          })
          const staffChart = echarts.init(this.$refs.staff)
          staffChart.setOption(this.staffOption)
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      // 获取统计数据
      queryCount().then(response => {
        if (response.code === 200) {
          this.countData[0].value = response.data.totalNum
          this.countData[1].value = response.data.normalNum
          this.countData[2].value = response.data.totalNum - response.data.normalNum
          this.countData[3].value = response.data.lateNum
          this.countData[4].value = response.data.leaveEarlyNum
          this.countData[5].value = response.data.absenteeismNum
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      // 获取城市设社保数据
      queryCity().then(response => {
        if (response.code === 200) {
          const cityData = response.data.map(item => [item.name, item.comPensionRate, item.comMedicalRate, item.comUnemploymentRate, item.comMaternityRate])
          cityData.unshift(['product', '养老保险', '医疗保险', '失业保险', '生育保险'])
          this.cityOption.dataset.source = cityData
          const cityChart = echarts.init(this.$refs.city)
          cityChart.setOption(this.cityOption)
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      // 获取当前月员工的考勤数据
      queryAttendance({ id: this.staff.id }).then(response => {
        if (response.code === 200) {
          this.attendanceData.list = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })

      queryDepartment().then(response => {
        if (response.code === 200) {
          this.departmentOption.series[0].data = response.data
          const departmentChart = echarts.init(this.$refs.department)
          departmentChart.setOption(this.departmentOption)
        } else {
          this.$message.error('获取数据失败！')
        }
      })
    },
    // 获取指定月份员工的考勤数据
    getStaffAttendance () {
      const d = this.attendanceData.date
      let month = ''
      if (d.getMonth() < 9) {
        month = d.getFullYear() + '0' + (d.getMonth() + 1)
      } else {
        month = d.getFullYear() + '' + (d.getMonth() + 1)
      }
      queryAttendance({ id: this.staff.id, month: month }).then(response => {
        if (response.code === 200) {
          this.attendanceData.list = response.data
        } else {
          this.$message.error('获取数据失败！')
        }
      })
    },
    handleUploadSuccess (res) {
      if (res.code === 200) {
        // 修改头像
        edit({ id: this.staff.id, avatar: res.data.name }).then(response => {
          if (response.code === 200) {
            this.$message.success('修改头像成功！')
            this.$store.commit('staff/SET_AVATAR', res.data.name)
          } else {
            this.$message.error('修改头像失败！')
          }
        })
      } else {
        this.$message.error(res.message)
      }
    }
  },
  created () {
    this.loading()
  }
}
</script>

<style>
.el-calendar-table .el-calendar-day {
  height: 54px;
}
</style>
